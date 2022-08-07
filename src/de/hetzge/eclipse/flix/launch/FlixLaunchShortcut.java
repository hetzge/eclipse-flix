package de.hetzge.eclipse.flix.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.PlatformUIUtils;
import de.hetzge.eclipse.utils.StatusUtils;

// https://www.vogella.com/tutorials/EclipseLauncherFramework/article.html

public class FlixLaunchShortcut implements ILaunchShortcut {

	private static final ILog LOG = Platform.getLog(FlixLaunchShortcut.class);

	@Override
	public void launch(ISelection selection, String mode) {
		System.out.println("FlixLaunchShortcut.launch(A)");
		launchProject(EclipseUtils.getFile(selection).orElseThrow(), mode);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		System.out.println("FlixLaunchShortcut.launch(B)");
		launchProject(EclipseUtils.getFile(editor).orElseThrow(), mode);
	}

	private void launchProject(IFile file, String mode) {
		if (file == null) {
			PlatformUIUtils.showError("Can't launch", "No launchable file selected");
			return;
		}

		final IProject project = file.getProject();
		final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		final ILaunchConfigurationType type = manager.getLaunchConfigurationType("de.hetzge.eclipse.flix.launchConfigurationType");

		try {
			// Find last launch configuration for file
			ILaunchConfiguration launchConfiguration = null;
			for (final ILaunchConfiguration configuration : manager.getLaunchConfigurations(type)) {
				final IResource[] mappedResources = configuration.getMappedResources();
				if (mappedResources != null) {
					for (final IResource resource : mappedResources) {
						if (resource.equals(file)) {
							launchConfiguration = configuration;
						}
					}
				}
			}

			if (launchConfiguration != null) {
				System.out.println("FlixLaunchShortcut.launchProject()");
				DebugUITools.launch(launchConfiguration, mode);
			} else {
				// Create new launch configuration
				final ILaunchConfigurationWorkingCopy copy = type.newInstance(null, String.format("Run '%s' in '%s'", file.getProjectRelativePath().toOSString().replaceAll("\\/", " "), project.getName()));
				copy.setMappedResources(new IResource[] { file });
				copy.doSave();
//				final int result = DebugUITools.openLaunchConfigurationDialog(PlatformUIUtil.getActiveShell(), copy, "eclipsezig.launchGroup", null);
//				if (result == Window.OK) {
//				} else {
//					PlatformUIUtil.showError("Can't launch", "Failed to create launch configuration.");
//					return;
//				}
			}
		} catch (final CoreException exception) {
			PlatformUIUtils.showError("Can't launch", "Something went wrong: " + exception.getMessage());
			LOG.log(StatusUtils.createError("Could not save new launch configuration", exception));
		}
	}
}
