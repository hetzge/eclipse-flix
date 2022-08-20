package de.hetzge.eclipse.flix.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.api.IFlixProject;

public class FlixLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigurationDelegate.class);

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		LOG.info(String.format("Launch '%s'", configuration.getName()));
		final IProject project = configuration.getMappedResources()[0].getProject();
		final IFlixProject flixProject = Flix.get().getModel().getFlixProject(project).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));
		final FlixLaunchConfiguration launchConfiguration = new FlixLaunchConfiguration(configuration);
		FlixLauncher.launchRun(launchConfiguration, flixProject);
	}

}
