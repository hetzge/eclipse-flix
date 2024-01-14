package de.hetzge.eclipse.flix;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.util.Disposable;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.lxtk.util.connect.Connectable.ConnectionState;

import de.hetzge.eclipse.flix.client.FlixLanguageClientController;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.server.FlixLanguageServerSocketThread;
import de.hetzge.eclipse.flix.server.FlixLanguageServerSocketThread.Status;
import de.hetzge.eclipse.utils.Utils;

public class FlixLanguageToolingManager implements AutoCloseable {

	private final Map<FlixProject, LanguageTooling> connectedProjects;

	public FlixLanguageToolingManager() {
		this.connectedProjects = new ConcurrentHashMap<>();
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

	public synchronized void connectProject(FlixProject flixProject) {
		System.out.println("FlixLanguageToolingManager.connectProject(" + flixProject.getProject().getName() + ")");
		this.connectedProjects.computeIfAbsent(flixProject, key -> {
			FlixLogger.logInfo("Initialize flix for project under " + flixProject.getProject().getLocationURI());
			return SafeRun.runWithResult(rollback -> {
				rollback.setLogger(FlixLogger::logError);
				final int lspPort = Utils.queryPort();
				final FlixLanguageServerSocketThread socketThread = FlixLanguageServerSocketThread.createAndStart(flixProject, lspPort);
				rollback.add(socketThread::close);
				final FlixLanguageClientController client = FlixLanguageClientController.connect(flixProject, lspPort);
				rollback.add(client::dispose);
				return new LanguageTooling(client, socketThread, rollback);
			});
		});
	}

	private synchronized void monitorProjects() {
		for (final Entry<FlixProject, LanguageTooling> entry : FlixLanguageToolingManager.this.connectedProjects.entrySet()) {
			System.out.println(entry.getKey().getProject().getName() + " " + entry.getValue().isHealthy());
			if (!entry.getValue().isHealthy()) {
				reconnectProject(entry.getKey());
			}
		}
	}

	public synchronized void disconnectProject(FlixProject flixProject) {
		System.out.println("FlixLanguageToolingManager.disconnectProject(" + flixProject.getProject().getName() + ")");
		Optional.ofNullable(this.connectedProjects.remove(flixProject)).ifPresent(LanguageTooling::close);
	}

	public synchronized void reconnectProject(FlixProject flixProject) {
		disconnectProject(flixProject);
		connectProject(flixProject);
	}

	public synchronized Optional<LanguageServer> getLanguageServerApi(FlixProject flixProject) {
		return Optional.ofNullable(this.connectedProjects.get(flixProject)).map(LanguageTooling::getLanguageServerApi);
	}

	@Override
	public synchronized void close() {
		for (final FlixProject flixProject : this.connectedProjects.keySet()) {
			disconnectProject(flixProject);
		}
	}

	private static class LanguageTooling implements AutoCloseable {

		private final FlixLanguageClientController controller;
		private final FlixLanguageServerSocketThread socketThread;
		private final Rollback rollback;

		public LanguageTooling(FlixLanguageClientController controller, FlixLanguageServerSocketThread socketThread, Rollback rollback) {
			this.controller = controller;
			this.socketThread = socketThread;
			this.rollback = rollback;
		}

		public LanguageServer getLanguageServerApi() {
			return this.controller.getLanguageServerApi();
		}

		public boolean isHealthy() {
			return this.controller.getConnectionState() != ConnectionState.DISCONNECTED && this.socketThread.isAlive() && this.socketThread.getStatus() != Status.STOPPED;
		}

		@Override
		public void close() {
			this.rollback.run();
		}
	}
}
