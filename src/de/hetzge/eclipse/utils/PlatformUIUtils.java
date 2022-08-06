package de.hetzge.eclipse.utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class PlatformUIUtils {

	private PlatformUIUtils() {
	}

	public static Shell getActiveShell() {
		final IWorkbenchWindow window = getActiveWorkbenchWindow();
		return window != null ? window.getShell() : null;
	}

	public static IWorkbenchPage getActivePage() {
		return getActiveWorkbenchWindow().getActivePage();
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getWorkbench().getActiveWorkbenchWindow();
	}

	public static IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}

	public static void showError(String title, String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog.openError(getActiveShell(), title, message);
		});
	}
}
