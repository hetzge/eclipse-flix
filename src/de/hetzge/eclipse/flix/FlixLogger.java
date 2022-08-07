package de.hetzge.eclipse.flix;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public final class FlixLogger {

	private FlixLogger() {
	}

	/**
	 * Utility method to log errors.
	 *
	 * @param throwable The exception through which we noticed the error
	 */
	public static void logError(final Throwable throwable) {
		Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, throwable.getMessage(), throwable));
	}

	/**
	 * Utility method to log errors.
	 *
	 * @param message   User comprehensible message
	 * @param throwable The exception through which we noticed the error
	 */
	public static void logError(final String message, final Throwable throwable) {
		Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, message, throwable));
	}

	/**
	 * Log an info message for this plug-in
	 *
	 * @param message
	 */
	public static void logInfo(final String message) {
		Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, 0, message, null));
	}

	/**
	 * Utility method to log warnings for this plug-in.
	 *
	 * @param message   User comprehensible message
	 * @param throwable The exception through which we noticed the warning
	 */
	public static void logWarning(final String message, final Throwable throwable) {
		Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, 0, message, throwable));
	}
}
