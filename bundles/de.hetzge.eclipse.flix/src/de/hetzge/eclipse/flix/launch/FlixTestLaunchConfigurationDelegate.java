package de.hetzge.eclipse.flix.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.utils.Utils;

public class FlixTestLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	// TODO JUnitViewEditorLauncher

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigurationDelegate.class);

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		LOG.info(String.format("Launch '%s'", configuration.getName()));

		final IProject project = configuration.getMappedResources()[0].getProject();
		final IFlixProject flixProject = Flix.get().getModelManager().getModel().getFlixProject(project).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));
		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = flixProject.getFlixCompilerJarFile();
		final String name = "Test " + flixProject.getProject().getName();

		final List<String> arguments = new ArrayList<>();
		arguments.add("-jar");
		arguments.add(flixJarFile.getAbsolutePath());
		arguments.add("test");

		final Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "de.hetzge.eclipse.flix.processLauncherDelegate");
		properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, name);
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, name);
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, jreExecutableFile.getAbsolutePath());
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, arguments.stream().collect(Collectors.joining(" ")));
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, project.getLocation().toFile().getAbsolutePath());

		final ITerminalService terminalService = TerminalServiceFactory.getService();
		terminalService.terminateConsole(properties, LOG::log);
		terminalService.closeConsole(properties, LOG::log);
		terminalService.openConsole(properties, LOG::log);
	}
}
