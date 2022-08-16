package de.hetzge.eclipse.flix;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.flix.client.FlixLanguageClient;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixModelManager;
import de.hetzge.eclipse.flix.server.FlixLanguageServerSocketThread;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

/**
 * The activator class controls the plug-in life cycle
 */
public class FlixActivator extends AbstractUIPlugin {

	private static FlixActivator plugin;

	public static FlixActivator getDefault() {
		Objects.requireNonNull(plugin, "Flix plugin is not initialized");
		return plugin;
	}

	private final Map<IProject, Rollback> connectedProjects;
	private Rollback rollback;
	private FlixModelManager modelManager;

	private Flix flix;

	public FlixActivator() {
		this.connectedProjects = new ConcurrentHashMap<>();
	}

	public Flix getFlix() {
		Objects.requireNonNull(this.flix, "Flix plugin is not initialized");
		return this.flix;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		SafeRun.run(rollback -> {
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			this.modelManager = FlixModelManager.setup(workspace);
			rollback.add(() -> {
				this.modelManager.close();
				this.modelManager = null;
			});
			this.flix = new Flix(this.modelManager);
			rollback.add(() -> {
				this.flix = null;
			});

			final FlixModel model = this.modelManager.getModel();
			model.getProjects().forEach(project -> {
				System.out.println(">>> " + project);
			});

			/*
			 * When open a document then check if the flix environment for this project is
			 * already initialized and initialize if necessary.
			 */
			rollback.add(FlixCore.DOCUMENT_SERVICE.onDidAddTextDocument().subscribe(document -> {
				EclipseUtils.getProject(document).ifPresent(this::initializeFlixProject);
			})::dispose);

			workspace.addResourceChangeListener(this.flix.getResourceMonitor(), IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> workspace.removeResourceChangeListener(this.flix.getResourceMonitor()));
			this.rollback = rollback;
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			System.out.println("Activator.stop()");
			for (final Rollback rollback : this.connectedProjects.values()) {
				rollback.run();
			}
			if (this.rollback != null) {
				this.rollback.run();
				this.rollback = null;
			}
			plugin = null;
		} finally {
			super.stop(context);
		}
	}

	private synchronized void initializeFlixProject(IProject project) {
		this.connectedProjects.computeIfAbsent(project, key -> {
			FlixLogger.logInfo("Initialize flix for project under " + project.getLocationURI());
			return SafeRun.runWithResult(rollback -> {

				final int lspPort = Utils.queryPort();
				final FlixLanguageServerSocketThread socketThread = FlixLanguageServerSocketThread.createAndStart(project, lspPort);
				rollback.add(socketThread::close);

				final FlixLanguageClient client = FlixLanguageClient.connect(project, lspPort);
				rollback.add(client::dispose);

				return rollback;
			});
		});
	}
}
