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

import de.hetzge.eclipse.utils.PlatformUIUtils;
import de.hetzge.eclipse.utils.StatusUtils;

class FlixLaunchUtils {

	private static final ILog LOG = Platform.getLog(FlixLaunchUtils.class);

	private FlixLaunchUtils() {
	}

	public static void launchProject(IFile file, String mode, String launchConfigurationTypeId, String label) {
		if (file == null) {
			PlatformUIUtils.showError("Can't launch", "No launchable file selected");
			return;
		}

		final IProject project = file.getProject();
		final ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		final ILaunchConfigurationType type = manager.getLaunchConfigurationType(launchConfigurationTypeId);

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
				final ILaunchConfigurationWorkingCopy copy = type.newInstance(null, String.format(label + " '%s' in '%s'", file.getProjectRelativePath().toOSString().replaceAll("\\/", " "), project.getName()));
				copy.setMappedResources(new IResource[] { file });
				copy.doSave();

				DebugUITools.launch(copy, mode);

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
