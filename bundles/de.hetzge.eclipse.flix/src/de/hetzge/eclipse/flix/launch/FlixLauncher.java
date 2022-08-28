package de.hetzge.eclipse.flix.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalServiceOutputStreamMonitorListener;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;

import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.utils.Utils;

public final class FlixLauncher {

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigurationDelegate.class);

	private FlixLauncher() {
	}

	public static void launchRun(FlixLaunchConfiguration launchConfiguration, IFlixProject flixProject) {
		launch(createTerminalRunProperties(launchConfiguration, flixProject));
	}

	public static void launchRepl(IFlixProject flixProject) {
		launch(createTerminalReplProperties(flixProject));
	}

	public static void launchTest(IFlixProject flixProject) {
		launch(createTerminalTestProperties(flixProject));
	}

	public static void launchBuild(IFlixProject flixProject) {
		launch(createTerminalBuildProperties(flixProject));
	}

	public static void launchCompiler(IFlixProject flixProject, int port) {
		final CountDownLatch latch = new CountDownLatch(1);
		final Map<String, Object> properties = createTerminalCompilerProperties(flixProject, port);
		properties.put(ITerminalsConnectorConstants.PROP_STDOUT_LISTENERS, new ITerminalServiceOutputStreamMonitorListener[] { new ITerminalServiceOutputStreamMonitorListener() {
			@Override
			public void onContentReadFromStream(byte[] byteBuffer, int bytesRead) {
				if (new String(byteBuffer).startsWith("LSP listening on")) {
					latch.countDown();
				}
			}
		} });
		launch(properties);
		try {
			latch.await(10L, TimeUnit.SECONDS);
		} catch (final InterruptedException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static void closeCompiler(IFlixProject flixProject, int port) {
		closeConsole(createTerminalCompilerProperties(flixProject, port));
	}

	private static void launch(Map<String, Object> properties) {
		if (properties.get(ITerminalsConnectorConstants.PROP_PROCESS_PATH) != null) {
			System.out.println(String.format("Launch '%s %s'", properties.get(ITerminalsConnectorConstants.PROP_PROCESS_PATH), properties.get(ITerminalsConnectorConstants.PROP_PROCESS_ARGS)));
		}

		final ITerminalService terminalService = TerminalServiceFactory.getService();
		terminalService.terminateConsole(properties, LOG::log);
		terminalService.closeConsole(properties, LOG::log);
		terminalService.openConsole(properties, LOG::log);
	}

	private static void closeConsole(Map<String, Object> properties) {
		final ITerminalService terminalService = TerminalServiceFactory.getService();
		terminalService.terminateConsole(properties, LOG::log);
		terminalService.closeConsole(properties, LOG::log);
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
		launchConfiguration.getArguments().ifPresent(launchArguments -> {
			arguments.add("--args");
			arguments.add(String.format("'%s'", launchArguments));
		});
		return createBasicTerminalLaunchProperties(name, flixProject, arguments);
	}

	private static Map<String, Object> createTerminalReplProperties(IFlixProject flixProject) {
		final String name = "Repl " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(name, flixProject, List.of());
	}

	private static Map<String, Object> createTerminalTestProperties(IFlixProject flixProject) {
		final String name = "Test " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(name, flixProject, List.of("test"));
	}

	private static Map<String, Object> createTerminalBuildProperties(IFlixProject flixProject) {
		final String name = "Build " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(name, flixProject, List.of("build"));
	}

	private static Map<String, Object> createTerminalCompilerProperties(IFlixProject flixProject, int port) {
		final String name = "Compiler " + flixProject.getProject().getName();
		final Map<String, Object> properties = createBasicTerminalLaunchProperties(name, flixProject, List.of("--lsp", String.valueOf(port)));
		properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, "Flix");
		return properties;
	}

	private static Map<String, Object> createBasicTerminalLaunchProperties(String name, IFlixProject flixProject, List<String> arguments) {
		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = flixProject.getFlixCompilerJarFile();

		final Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "de.hetzge.eclipse.flix.processLauncherDelegate");
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, name);
		properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, null);
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, jreExecutableFile.getAbsolutePath());
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, Stream.concat(Stream.of("-jar", flixJarFile.getAbsolutePath()), arguments.stream()).collect(Collectors.joining(" ")));
		if (flixProject.getProject().getLocation() != null && flixProject.getProject().getLocation().toFile() != null) {
			properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, flixProject.getProject().getLocation().toFile().getAbsolutePath());
		}
		return properties;
	}
}
