package de.hetzge.eclipse.flix;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import de.hetzge.eclipse.flix.client.FlixLanguageClientController;
import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.flix.server.FlixLanguageServerSocketThread;
import de.hetzge.eclipse.utils.Utils;

public class FlixLanguageToolingManager implements AutoCloseable {

	private final Map<IFlixProject, Rollback> connectedProjects;

	public FlixLanguageToolingManager() {
		this.connectedProjects = new ConcurrentHashMap<>();
	}

	public synchronized void connectProject(IFlixProject flixProject) {
		System.out.println("FlixLanguageToolingManager.initializeFlixProject(" + flixProject.getProject().getName() + ")");
		this.connectedProjects.computeIfAbsent(flixProject, key -> {
			FlixLogger.logInfo("Initialize flix for project under " + flixProject.getProject().getLocationURI());
			return SafeRun.runWithResult(rollback -> {

				final int lspPort = Utils.queryPort();
				final FlixLanguageServerSocketThread socketThread = FlixLanguageServerSocketThread.createAndStart(flixProject, lspPort);
				rollback.add(socketThread::close);

				final FlixLanguageClientController client = FlixLanguageClientController.connect(flixProject, lspPort);
				rollback.add(client::dispose);

				return rollback;
			});
		});
	}

	public synchronized void disconnectProject(IFlixProject flixProject) {
		System.out.println("FlixLanguageToolingManager.closeProject(" + flixProject.getProject().getName() + ")");
		Optional.ofNullable(this.connectedProjects.remove(flixProject)).ifPresent(Rollback::run);
	}

	public synchronized void reconnectProject(IFlixProject flixProject) {
		disconnectProject(flixProject);
		connectProject(flixProject);
	}

	@Override
	public synchronized void close() {
		for (final IFlixProject flixProject : this.connectedProjects.keySet()) {
			disconnectProject(flixProject);
		}
	}
}
