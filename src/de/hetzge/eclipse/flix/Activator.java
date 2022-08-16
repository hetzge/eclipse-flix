package de.hetzge.eclipse.flix;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
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
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "de.hetzge.eclipse.flix"; //$NON-NLS-1$

	private static Activator plugin;

	private ResourceMonitor resourceMonitor;
	private FlixDocumentProvider flixDocumentProvider;
	private final Map<IProject, Rollback> connectedProjects;
	private Rollback rollback;
	private FlixModelManager modelManager;

	public Activator() {
		this.connectedProjects = new ConcurrentHashMap<>();
	}

	public ResourceMonitor getResourceMonitor() {
		return this.resourceMonitor;
	}

	public FlixDocumentProvider getFlixDocumentProvider() {
		return this.flixDocumentProvider;
	}

	public FlixModelManager getModelManager() {
		if (this.modelManager == null) {
			throw new IllegalStateException("Flix plugin is not initialized");
		}
		return this.modelManager;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		SafeRun.run(rollback -> {
			this.flixDocumentProvider = new FlixDocumentProvider();
			this.modelManager = FlixModelManager.setup();
			rollback.add(() -> {
				this.modelManager.close();
				this.modelManager = null;
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

			this.resourceMonitor = new ResourceMonitor();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this.resourceMonitor, IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.resourceMonitor));
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

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
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
