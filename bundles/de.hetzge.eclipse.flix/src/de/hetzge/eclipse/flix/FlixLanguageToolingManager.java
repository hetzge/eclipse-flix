package de.hetzge.eclipse.flix;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.ui.PlatformUI;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.lxtk.util.connect.Connectable.ConnectionState;

import de.hetzge.eclipse.flix.client.FlixLanguageClientController;
import de.hetzge.eclipse.flix.compiler.FlixCompilerClient;
import de.hetzge.eclipse.flix.compiler.FlixCompilerService;
import de.hetzge.eclipse.flix.launch.FlixLauncher;
import de.hetzge.eclipse.flix.manifest.FlixManifestToml;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.navigator.FlixLanguageToolingStateDecorator;
import de.hetzge.eclipse.flix.server.FlixLanguageServerSocketThread;
import de.hetzge.eclipse.flix.server.FlixLanguageServerSocketThread.Status;
import de.hetzge.eclipse.flix.server.FlixMiddlewareLanguageServer;
import de.hetzge.eclipse.utils.Utils;

public class FlixLanguageToolingManager implements AutoCloseable {

	private final Map<FlixProject, LanguageTooling> connectedProjects;
	private final EventEmitter<Status> statusChangedEventEmitter;

	public FlixLanguageToolingManager() {
		this.connectedProjects = new ConcurrentHashMap<>();
		this.statusChangedEventEmitter = new EventEmitter<>();
	}

	public Disposable startMonitor() {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					monitorProjects();
					try {
						Thread.sleep(1000L);
					} catch (final InterruptedException exception) {
						break;
					}
				}
			}
		};
		thread.setName("Flix language tooling monitor thread"); //$NON-NLS-1$
		thread.setDaemon(true);
		thread.start();
		return () -> thread.interrupt();
	}

	public void connectProjects(List<FlixProject> projects) {
		for (final FlixProject project : projects) {
			connectProject(project);
		}
	}

	public synchronized void connectProject(FlixProject project) {
		refreshFlixDependencies(project);
		project.createOrGetStandardLibraryFolder(new NullProgressMonitor());
		final Job job = Job.create("Connect Flix language tooling: " + project.getProject().getName(), (ICoreRunnable) monitor -> {
			this.connectedProjects.computeIfAbsent(project, ignore -> {
				return SafeRun.runWithResult(rollback -> {
					rollback.setLogger(FlixLogger::logError);
					final FlixModel model = Flix.get().getModel();
					Flix.get().getWorkspaceService().setWorkspaceFolders(model.getWorkspaceFolders());
					final Supplier<Boolean> isRunning;
					final FlixCompilerClient compilerClient;
					if (!Objects.equals(System.getProperty("flix.debug"), "true")) {
						final int compilerPort = Utils.queryPort();

						final CountDownLatch readyLatch = new CountDownLatch(1);
						final CountDownLatch connectedLatch = new CountDownLatch(1);

						final Process lspProcess = FlixLauncher.launchLsp(project, compilerPort, text -> {
							if (text.startsWith("Listen on")) { //$NON-NLS-1$
								readyLatch.countDown();
							} else if (text.startsWith("Client at")) { //$NON-NLS-1$
								connectedLatch.countDown();
							}
						});
						isRunning = () -> lspProcess.isAlive();
						rollback.add(lspProcess::destroyForcibly);
						final int connectPort = compilerPort;
						try {
							readyLatch.await(1L, TimeUnit.MINUTES);
						} catch (final InterruptedException exception) {
							throw new RuntimeException("Failed to start Flix language server", exception);
						}
						compilerClient = FlixCompilerClient.connect(connectPort);
						rollback.add(compilerClient::close);
						try {
							connectedLatch.await(1L, TimeUnit.MINUTES);
						} catch (final InterruptedException exception) {
							throw new RuntimeException("Failed to connect to Flix compiler", exception);
						}
					} else {
						compilerClient = FlixCompilerClient.connect(32323);
						isRunning = () -> true;
					}
					final FlixCompilerService compilerService = new FlixCompilerService(project, compilerClient);
					final FlixMiddlewareLanguageServer server = new FlixMiddlewareLanguageServer(compilerService);
					final int lspPort = Utils.queryPort();
					final FlixLanguageServerSocketThread socketThread = new FlixLanguageServerSocketThread(project, server, isRunning, lspPort);
					rollback.add(socketThread::close);
					socketThread.start();
					final FlixLanguageClientController controller = FlixLanguageClientController.connect(project, lspPort);
					rollback.add(controller::dispose);
					return new LanguageTooling(controller, compilerService, socketThread, rollback);
				});
			});
			compile(project);
		});
		job.setRule(project.getProject());
		job.schedule();
	}

	private void refreshFlixDependencies(FlixProject project) {
		final Job dependenciesJob = Job.create("Refresh Flix dependencies: " + project.getProject().getName(), (ICoreRunnable) monitor -> {
			project.refreshProjectFolders(monitor);
			final String libHash = project.calculateLibHash();
			final String lastLibHash = project.getLastLibHash().orElse(null);
			final String dependencyHash = project.getFlixToml().map(FlixManifestToml::calculateDependencyHash).orElse(null);
			final String lastDependencyHash = project.getLastDependencyHash().orElse(null);
			System.out.println("Lib hash: " + libHash + ", lastLib hash: " + lastLibHash + ", dependency hash: " + dependencyHash + ", last dependency hash: " + lastDependencyHash);
			if (libHash != null && dependencyHash != null && (!Objects.equals(lastDependencyHash, dependencyHash) || !Objects.equals(lastLibHash, libHash))) {
				project.setLastDependencyHash(dependencyHash);
				project.setLastLibHash(libHash);
				project.deleteTemporaryFolders(monitor);
				final Process process = FlixLauncher.launchOutdated(project);
				Utils.waitForProcess(process);
				project.refreshProjectFolders(monitor);
			}
		});
		dependenciesJob.setRule(project.getProject());
		dependenciesJob.schedule();
	}

	public synchronized void disconnectProjects() {
		for (final FlixProject project : this.connectedProjects.keySet()) {
			disconnectProject(project);
		}
	}

	public synchronized void disconnectProject(FlixProject project) {
		final Job job = Job.create("Disconnect Flix language tooling: " + project.getProject().getName(), (ICoreRunnable) monitor -> {
			final LanguageTooling languageTooling = this.connectedProjects.remove(project);
			// Close language tooling
			if (languageTooling != null) {
				languageTooling.close();
			}
			// Delete compiler folder
			project.deleteFlixCompilerFolder(monitor);
		});
		job.setRule(project.getProject());
		job.schedule();
	}

	public synchronized void reconnectProject(FlixProject project) {
		disconnectProject(project);
		connectProject(project);
	}

	private synchronized void monitorProjects() {
		for (final Entry<FlixProject, LanguageTooling> entry : this.connectedProjects.entrySet()) {
			final FlixProject flixProject = entry.getKey();
			final Boolean reconnect = PlatformUI.getWorkbench().getDisplay().syncCall(() -> {
				if (!entry.getValue().isHealthy()) {
					if (MessageDialog.openConfirm(PlatformUI.getWorkbench().getDisplay().getActiveShell(), String.format("Flix language tooling crashed for project '%s'", flixProject.getProject().getName()), "Try to restart?")) {
						return Boolean.TRUE;
					} else {
						return Boolean.FALSE;
					}
				}
				return null;
			});
			if (Objects.equals(reconnect, Boolean.TRUE)) {
				reconnectProject(flixProject);
			} else if (Objects.equals(reconnect, Boolean.FALSE)) {
				disconnectProject(flixProject);
			}
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			PlatformUI.getWorkbench().getDecoratorManager().update(FlixLanguageToolingStateDecorator.ID);
		});
	}

	private Optional<LanguageTooling> getLanguageTooling(FlixProject flixProject) {
		return Optional.ofNullable(this.connectedProjects.get(flixProject));
	}

	public void compile(FlixProject project) {
		final Job job = Job.create("Compile Flix project: " + project.getProject().getName(), (ICoreRunnable) monitor -> {
			final LanguageTooling languageTooling = this.connectedProjects.get(project);
			if (languageTooling != null) {
				languageTooling.getCompilerService().addWorkspaceUris();
				languageTooling.getCompilerService().syncCompile();
			}
			project.refreshProjectFolders(monitor);
		});
		job.setRule(project.getProject());
		job.schedule();
	}

	@Override
	public synchronized void close() {
		try {
			disconnectProjects();
		} finally {
			this.statusChangedEventEmitter.dispose();
		}
	}

	public boolean isStarted(FlixProject project) {
		return getLanguageTooling(project).map(LanguageTooling::isStarted).orElse(false);
	}

	public EventEmitter<Status> onStatusChanged() {
		return this.statusChangedEventEmitter;
	}

	private static class LanguageTooling implements AutoCloseable {

		private final FlixLanguageClientController controller;
		private final FlixCompilerService compilerService;
		private final FlixLanguageServerSocketThread socketThread;
		private final Rollback rollback;

		public LanguageTooling(FlixLanguageClientController controller, FlixCompilerService compilerService, FlixLanguageServerSocketThread socketThread, Rollback rollback) {
			this.controller = controller;
			this.compilerService = compilerService;
			this.socketThread = socketThread;
			this.rollback = rollback;
		}

		public LanguageServer getLanguageServerApi() {
			return this.controller.getLanguageServerApi();
		}

		public FlixCompilerService getCompilerService() {
			return this.compilerService;
		}

		public boolean isHealthy() {
			return this.controller.getConnectionState() != ConnectionState.DISCONNECTED && this.socketThread.isAlive() && this.socketThread.getStatus() != Status.STOPPED;
		}

		public boolean isStarted() {
			return isHealthy() && this.socketThread.getStatus() == Status.STARTED;
		}

		@Override
		public void close() {
			this.rollback.run();
		}
	}
}
