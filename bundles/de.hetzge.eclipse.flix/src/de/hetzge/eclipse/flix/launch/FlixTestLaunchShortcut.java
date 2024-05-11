package de.hetzge.eclipse.flix.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixTestLaunchShortcut implements ILaunchShortcut {

	@Override
	public void launch(ISelection selection, String mode) {
		launch(mode, EclipseUtils.getFile(selection).orElseThrow());
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		launch(mode, EclipseUtils.getFile(editor).orElseThrow());
	}

	private void launch(String mode, IFile file) {
		FlixLaunchUtils.launchProject(file, mode, FlixConstants.TEST_LAUNCH_CONFIGURATION_TYPE_ID, null);
	}
}
