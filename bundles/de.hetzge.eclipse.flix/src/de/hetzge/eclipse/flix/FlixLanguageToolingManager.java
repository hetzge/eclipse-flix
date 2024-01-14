package de.hetzge.eclipse.flix;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.ui.PlatformUI;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.lxtk.util.connect.Connectable.ConnectionState;

import de.hetzge.eclipse.flix.client.FlixLanguageClientController;
import de.hetzge.eclipse.flix.compiler.FlixCompilerClient;
import de.hetzge.eclipse.flix.compiler.FlixCompilerLaunch;
import de.hetzge.eclipse.flix.compiler.FlixCompilerLaunchConfigurationDelegate;
import de.hetzge.eclipse.flix.compiler.FlixCompilerService;
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
		thread.setName("Flix language tooling monitor thread");
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
		this.connectedProjects.computeIfAbsent(project, ignore -> {
			return SafeRun.runWithResult(rollback -> {
				rollback.setLogger(FlixLogger::logError);
				final FlixModel model = Flix.get().getModel();
				Flix.get().getWorkspaceService().setWorkspaceFolders(model.getWorkspaceFolders());
				rollback.setLogger(FlixLogger::logError);
				final int compilerPort = Utils.queryPort();
				final FlixCompilerLaunch launch = FlixCompilerLaunchConfigurationDelegate.launch(project, compilerPort);
				rollback.add(launch::dispose);
				launch.waitUntilReady();
				final FlixCompilerClient compilerClient = FlixCompilerClient.connect(compilerPort);
				rollback.add(compilerClient::close);
				launch.waitUntilConnected();
				final FlixCompilerService compilerService = new FlixCompilerService(project, compilerClient);
				final FlixMiddlewareLanguageServer server = new FlixMiddlewareLanguageServer(compilerService, launch);
				final int lspPort = Utils.queryPort();
				final FlixLanguageServerSocketThread socketThread = new FlixLanguageServerSocketThread(project, server, lspPort);
				rollback.add(socketThread::close);
				socketThread.start();
				final FlixLanguageClientController controller = FlixLanguageClientController.connect(project, lspPort);
				rollback.add(controller::dispose);
				return new LanguageTooling(controller, compilerService, socketThread, rollback);
			});
		});
	}

	public synchronized void disconnectProjects() {
		for (final FlixProject project : this.connectedProjects.keySet()) {
			disconnectProject(project);
		}
	}

	public synchronized void disconnectProject(FlixProject project) {
		final LanguageTooling languageTooling = this.connectedProjects.remove(project);
		if (languageTooling != null) {
			languageTooling.close();
		}
	}

	public synchronized void reconnectProject(FlixProject project) {
		disconnectProject(project);
		connectProject(project);
	}

	private synchronized void monitorProjects() {
		for (final Entry<FlixProject, LanguageTooling> entry : this.connectedProjects.entrySet()) {
			if (!entry.getValue().isHealthy()) {
				reconnectProject(entry.getKey());
			}
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				PlatformUI.getWorkbench().getDecoratorManager().update(FlixLanguageToolingStateDecorator.ID);
			});
		}
	}

	public synchronized Optional<LanguageServer> getLanguageServerApi(FlixProject flixProject) {
		return Optional.ofNullable(this.connectedProjects.get(flixProject)).map(LanguageTooling::getLanguageServerApi);
	}

	public void compile(FlixProject project) {
		final LanguageTooling languageTooling = this.connectedProjects.get(project);
		if (languageTooling != null) {
			languageTooling.getCompilerService().addWorkspaceUris();
			languageTooling.getCompilerService().compile();
		}
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
		return Optional.ofNullable(this.connectedProjects.get(project)).map(LanguageTooling::isStarted).orElse(false);
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
