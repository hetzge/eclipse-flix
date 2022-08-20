package de.hetzge.eclipse.flix.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;

import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.utils.Utils;

public final class FlixLauncher {

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigurationDelegate.class);

	private FlixLauncher() {
	}

	public static void launchRun(FlixLaunchConfiguration launchConfiguration, final IFlixProject flixProject) {
		launch(createTerminalRunProperties(launchConfiguration, flixProject));
	}

	public static void launchTest(final IFlixProject flixProject) {
		launch(createTerminalTestProperties(flixProject));
	}

	public static void launchBuild(final IFlixProject flixProject) {
		launch(createTerminalBuildProperties(flixProject));
	}

	private static void launch(Map<String, Object> properties) {
		final ITerminalService terminalService = TerminalServiceFactory.getService();
		terminalService.terminateConsole(properties, LOG::log);
		terminalService.closeConsole(properties, LOG::log);
		terminalService.openConsole(properties, LOG::log);
	}

	private static Map<String, Object> createTerminalRunProperties(FlixLaunchConfiguration launchConfiguration, final IFlixProject flixProject) {
		final String name = "Run " + flixProject.getProject().getName();
		final List<String> arguments = new ArrayList<>();
		arguments.add("run");
		launchConfiguration.getEntrypoint().ifPresent(entrypoint -> {
			arguments.add("--entrypoint");
			arguments.add(entrypoint);
		});
		for (final IFile sourceFile : flixProject.getFlixSourceFiles()) {
			arguments.add(sourceFile.getFullPath().toFile().getAbsolutePath());
		}
		for (final IFile libraryFile : flixProject.getFlixFpkgLibraryFiles()) {
			arguments.add(libraryFile.getFullPath().toFile().getAbsolutePath());
		}
		for (final IFile libraryFile : flixProject.getFlixJarLibraryFiles()) {
			arguments.add(libraryFile.getFullPath().toFile().getAbsolutePath());
		}
		return createBasicTerminalLaunchProperties(flixProject, arguments, name);
	}

	private static Map<String, Object> createTerminalTestProperties(IFlixProject flixProject) {
		final String name = "Test " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(flixProject, List.of("test"), name);
	}

	private static Map<String, Object> createTerminalBuildProperties(IFlixProject flixProject) {
		final String name = "Build " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(flixProject, List.of("build"), name);
	}

	private static Map<String, Object> createBasicTerminalLaunchProperties(IFlixProject flixProject, List<String> arguments, String name) {
		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = flixProject.getFlixCompilerJarFile();
		final Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "de.hetzge.eclipse.flix.processLauncherDelegate");
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, name);
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, jreExecutableFile.getAbsolutePath());
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, Stream.concat(Stream.of("-jar", flixJarFile.getAbsolutePath()), arguments.stream()).collect(Collectors.joining(" ")));
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, flixProject.getProject().getLocation().toFile().getAbsolutePath());
		return properties;
	}

}
