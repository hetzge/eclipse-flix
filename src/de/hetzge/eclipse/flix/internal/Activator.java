package de.hetzge.eclipse.flix.internal;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

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

	private Rollback rollback;

	public ResourceMonitor getResourceMonitor() {
		return this.resourceMonitor;
	}

	public FlixDocumentProvider getFlixDocumentProvider() {
		return this.flixDocumentProvider;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		SafeRun.run(rollback -> {
			final FlixService flixService = new FlixService();
			rollback.add(flixService::close);
			flixService.initialize();
			final FlixLanguageClient flixLanguageClient = new FlixLanguageClient(ResourcesPlugin.getWorkspace().getRoot().getProjects()[0], flixService);
			flixLanguageClient.connect();

			this.resourceMonitor = new ResourceMonitor();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this.resourceMonitor, IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.resourceMonitor));
			this.flixDocumentProvider = new FlixDocumentProvider();
			this.rollback = rollback;
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
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

	/**
	 * Utility method to log errors.
	 *
	 * @param thr The exception through which we noticed the error
	 */
	public static void logError(final Throwable thr) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, thr.getMessage(), thr));
	}

	/**
	 * Utility method to log errors.
	 *
	 * @param message User comprehensible message
	 * @param thr     The exception through which we noticed the error
	 */
	public static void logError(final String message, final Throwable thr) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, thr));
	}

	/**
	 * Log an info message for this plug-in
	 *
	 * @param message
	 */
	public static void logInfo(final String message) {
		getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, 0, message, null));
	}

	/**
	 * Utility method to log warnings for this plug-in.
	 *
	 * @param message User comprehensible message
	 * @param thr     The exception through which we noticed the warning
	 */
	public static void logWarning(final String message, final Throwable thr) {
		getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, 0, message, thr));
	}

}
