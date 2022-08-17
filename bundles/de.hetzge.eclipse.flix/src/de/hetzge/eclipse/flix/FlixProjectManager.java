package de.hetzge.eclipse.flix;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import de.hetzge.eclipse.flix.client.FlixLanguageClientController;
import de.hetzge.eclipse.flix.server.FlixLanguageServerSocketThread;
import de.hetzge.eclipse.utils.Utils;

public class FlixProjectManager implements AutoCloseable {

	private final Map<IProject, Rollback> connectedProjects;

	public FlixProjectManager() {
		this.connectedProjects = new ConcurrentHashMap<>();
	}

	public synchronized void initializeFlixProject(IProject project) {
		this.connectedProjects.computeIfAbsent(project, key -> {
			FlixLogger.logInfo("Initialize flix for project under " + project.getLocationURI());
			return SafeRun.runWithResult(rollback -> {

				final int lspPort = Utils.queryPort();
				final FlixLanguageServerSocketThread socketThread = FlixLanguageServerSocketThread.createAndStart(project, lspPort);
				rollback.add(socketThread::close);

				final FlixLanguageClientController client = FlixLanguageClientController.connect(project, lspPort);
				rollback.add(client::dispose);

				return rollback;
			});
		});
	}

	@Override
	public void close() {
		for (final Rollback rollback : this.connectedProjects.values()) {
			rollback.run();
		}
	}
}
