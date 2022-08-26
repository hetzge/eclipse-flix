package de.hetzge.eclipse.flix.launch;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.utils.PlatformUIUtils;
import de.hetzge.eclipse.utils.StatusUtils;

class FlixLaunchUtils {

	private static final ILog LOG = Platform.getLog(FlixLaunchUtils.class);

	private FlixLaunchUtils() {
	}

	public static void launchProject(IFile file, String mode, String launchConfigurationTypeId, String entrypoint) {
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
				if (Optional.of(file).equals(getFile(configuration)) && Optional.ofNullable(entrypoint).equals(new FlixLaunchConfiguration(configuration).getEntrypoint())) {
					launchConfiguration = configuration;
				}
			}

			if (launchConfiguration == null) {
				// Create new launch configuration
				final ILaunchConfigurationWorkingCopy newLaunchConfiguration = type.newInstance(null, String.format(getLabel(launchConfigurationTypeId) + " '%s' in '%s'", file.getProjectRelativePath().toOSString(), project.getName()).replace(File.separatorChar, ' '));
				final EditableFlixLaunchConfiguration flixLaunchConfiguration = new EditableFlixLaunchConfiguration(newLaunchConfiguration);
				flixLaunchConfiguration.setEntrypoint(entrypoint);
				newLaunchConfiguration.setMappedResources(new IResource[] { file });
				newLaunchConfiguration.doSave();
				launchConfiguration = newLaunchConfiguration;
			}

			LOG.info(String.format("Launch '%s'", launchConfiguration.getName()));
			DebugUITools.launch(launchConfiguration, mode);
		} catch (final CoreException exception) {
			PlatformUIUtils.showError("Can't launch", "Something went wrong: " + exception.getMessage());
			LOG.log(StatusUtils.createError("Could not save new launch configuration", exception));
		}
	}

	private static Optional<IFile> getFile(ILaunchConfiguration configuration) {
		return SafeRunner.run(() -> {
			final IResource[] resources = configuration.getMappedResources();
			if (resources.length > 0 && resources[0] instanceof IFile) {
				return Optional.of(IFile.class.cast(resources[0]));
			} else {
				return Optional.empty();
			}
		});
	}

	private static String getLabel(String launchConfigurationTypeId) {
		final String label;
		if (launchConfigurationTypeId.equals(FlixConstants.LAUNCH_CONFIGURATION_TYPE_ID)) {
			label = "Run";
		} else if (launchConfigurationTypeId.equals(FlixConstants.TEST_LAUNCH_CONFIGURATION_TYPE_ID)) {
			label = "Test";
		} else {
			label = "Execute";
		}
		return label;
	}
}
