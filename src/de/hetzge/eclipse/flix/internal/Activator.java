package de.hetzge.eclipse.flix.internal;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.flix.FlixCore;
import de.hetzge.eclipse.utils.EclipseUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "de.hetzge.eclipse.flix"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private ResourceMonitor resourceMonitor;

	private FlixDocumentProvider flixDocumentProvider;

	private final Map<IProject, FlixProject> connectedProjects;

	private Rollback rollback;

	public Activator() {
		this.connectedProjects = new ConcurrentHashMap<>();
	}

	public ResourceMonitor getResourceMonitor() {
		return this.resourceMonitor;
	}

	public FlixDocumentProvider getFlixDocumentProvider() {
		return this.flixDocumentProvider;
	}

	public synchronized FlixProject getFlixProject(IProject project) {
		return this.connectedProjects.computeIfAbsent(project, key -> {
			FlixLogger.logInfo("Initialize flix for project under " + project.getLocationURI());
			return SafeRun.runWithResult(rollback -> {
				final int compilerPort = queryPort();
				final int lspPort = queryPort();
				final FlixService flixService = new FlixService(project);
				this.rollback.add(flixService::close);
				flixService.initialize(compilerPort, lspPort);
				final FlixLanguageClient flixLanguageClient = new FlixLanguageClient(project, flixService, lspPort);
				rollback.add(flixLanguageClient::dispose);
				flixLanguageClient.connect();
				return new FlixProject(flixService, flixLanguageClient);
			});
		});
	}

	private Integer queryPort() {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		SafeRun.run(rollback -> {

			/*
			 * When open a document then check if the flix environment for this project is
			 * already initialized and initialize if necessary.
			 */
			rollback.add(FlixCore.DOCUMENT_SERVICE.onDidAddTextDocument().subscribe(document -> {
				EclipseUtils.getProject(document).ifPresent(this::getFlixProject);
			})::dispose);

			this.resourceMonitor = new ResourceMonitor();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this.resourceMonitor, IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.resourceMonitor));
			this.flixDocumentProvider = new FlixDocumentProvider();
			this.rollback = rollback;
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		for (final FlixProject project : this.connectedProjects.values()) {
			project.close();
		}
		if (this.rollback != null) {
			this.rollback.reset();
			this.rollback = null;
		}
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
}
