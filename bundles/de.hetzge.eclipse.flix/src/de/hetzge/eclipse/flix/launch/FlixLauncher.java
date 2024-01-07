package de.hetzge.eclipse.flix.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService.Done;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalServiceOutputStreamMonitorListener;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.model.FlixVersion;
import de.hetzge.eclipse.flix.utils.FlixUtils;
import de.hetzge.eclipse.utils.Utils;

public final class FlixLauncher {

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigurationDelegate.class);
	private static final Map<String, Map<String, Object>> PROPERTIES_BY_KEY = new ConcurrentHashMap<>();

	private FlixLauncher() {
	}

	public static void launchRun(FlixLaunchConfiguration launchConfiguration, FlixProject flixProject) {
		launch(createTerminalRunProperties(launchConfiguration, flixProject), String.format("RUN_%s", flixProject.getProject().getFullPath().toOSString()));
	}

	public static void launchRepl(FlixProject flixProject) {
		launch(createTerminalReplProperties(flixProject), String.format("REPL_%s", flixProject.getProject().getFullPath().toOSString()));
	}

	public static void launchTest(FlixProject flixProject) {
		launch(createTerminalTestProperties(flixProject), String.format("TEST_%s", flixProject.getProject().getFullPath().toOSString()));
	}

	public static void launchBuild(FlixProject flixProject) {
		launch(createTerminalBuildProperties(flixProject), String.format("BUILD_%s", flixProject.getProject().getFullPath().toOSString()));
	}

	public static void launchCompiler(FlixProject flixProject, int port) {
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
		launch(properties, String.format("COMPILER_%s", flixProject.getProject().getFullPath().toOSString()));
		try {
			latch.await(10L, TimeUnit.SECONDS);
		} catch (final InterruptedException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static void launchInit(File folder, FlixVersion flixVersion) {
		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = FlixUtils.loadFlixJarFile(flixVersion, null);
		final ProcessBuilder processBuilder = new ProcessBuilder(jreExecutableFile.getAbsolutePath(), "-jar", flixJarFile.getAbsolutePath(), "init");
		processBuilder.directory(folder);
		try {
			final Process process = processBuilder.start();
			final Map<String, Object> properties = createTerminalInitProperties(folder);
			properties.remove(ITerminalsConnectorConstants.PROP_PROCESS_PATH);
			properties.put(ITerminalsConnectorConstants.PROP_PROCESS_OBJ, process);
			launch(properties, UUID.randomUUID().toString());
			process.waitFor();
		} catch (final IOException | InterruptedException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static void closeCompiler(FlixProject flixProject, int port) {
		System.out.println("FlixLauncher.closeCompiler(!!!)");
		closeConsole(createTerminalCompilerProperties(flixProject, port));
	}

	private static void launch(Map<String, Object> properties, String launchKey) {
		if (properties.get(ITerminalsConnectorConstants.PROP_PROCESS_PATH) != null) {
			System.out.println(String.format("Launch '%s %s'", properties.get(ITerminalsConnectorConstants.PROP_PROCESS_PATH), properties.get(ITerminalsConnectorConstants.PROP_PROCESS_ARGS)));
		}

		final Map<String, Object> lastProperties = PROPERTIES_BY_KEY.getOrDefault(launchKey, properties);
		final ITerminalService terminalService = TerminalServiceFactory.getService();
		terminalService.terminateConsole(lastProperties, createDoneLogger("terminateConsole"));
		terminalService.closeConsole(lastProperties, createDoneLogger("closeConsole"));
		terminalService.openConsole(properties, createDoneLogger("openConsole"));
		PROPERTIES_BY_KEY.put(launchKey, properties);
	}

	private static void closeConsole(Map<String, Object> properties) {
		final ITerminalService terminalService = TerminalServiceFactory.getService();
		terminalService.terminateConsole(properties, createDoneLogger("terminateConsole"));
		terminalService.closeConsole(properties, createDoneLogger("closeConsole"));
	}

	private static Map<String, Object> createTerminalRunProperties(FlixLaunchConfiguration launchConfiguration, final FlixProject flixProject) {
		final String name = "Run " + flixProject.getProject().getName();
		final List<String> arguments = new ArrayList<>();
		launchConfiguration.getEntrypoint().ifPresent(entrypoint -> {
			arguments.add("--entrypoint");
			arguments.add(entrypoint);
		});
		for (final IFile sourceFile : flixProject.getFlixSourceFiles()) {
			arguments.add(new File(sourceFile.getLocationURI()).getAbsolutePath());
		}
		for (final IFile libraryFile : flixProject.getFlixFpkgLibraryFiles()) {
			arguments.add(new File(libraryFile.getLocationURI()).getAbsolutePath());
		}
		for (final IFile libraryFile : flixProject.getFlixJarLibraryFiles()) {
			arguments.add(new File(libraryFile.getLocationURI()).getAbsolutePath());
		}
		launchConfiguration.getArguments().ifPresent(launchArguments -> {
			arguments.add("--args");
			arguments.add(String.format("'%s'", launchArguments));
		});
		return createBasicTerminalLaunchProperties(name, flixProject, arguments);
	}

	private static Map<String, Object> createTerminalReplProperties(FlixProject flixProject) {
		final String name = "Repl " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(name, flixProject, List.of());
	}

	private static Map<String, Object> createTerminalTestProperties(FlixProject flixProject) {
		final String name = "Test " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(name, flixProject, List.of("test"));
	}

	private static Map<String, Object> createTerminalBuildProperties(FlixProject flixProject) {
		final String name = "Build " + flixProject.getProject().getName();
		return createBasicTerminalLaunchProperties(name, flixProject, List.of("build"));
	}

	private static Map<String, Object> createTerminalCompilerProperties(FlixProject flixProject, int port) {
		final String name = "Compiler (" + flixProject.getFlixVersion().getKey() + ") " + flixProject.getProject().getName();
		final Map<String, Object> properties = createBasicTerminalLaunchProperties(name, flixProject, List.of("lsp", String.valueOf(port)));
		properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, "Flix");
		return properties;
	}

	private static Map<String, Object> createTerminalInitProperties(File folder) {
		final String name = "Init " + folder.getName();
		return createBasicTerminalLaunchProperties(name, folder, FlixUtils.loadFlixJarFile(FlixConstants.FLIX_DEFAULT_VERSION, null), List.of("init"));
	}

	private static Map<String, Object> createBasicTerminalLaunchProperties(String name, FlixProject flixProject, List<String> arguments) {
		return createBasicTerminalLaunchProperties(name, flixProject.getProject().getLocation() != null ? flixProject.getProject().getLocation().toFile() : null, flixProject.getFlixCompilerJarFile(), arguments);
	}

	private static Map<String, Object> createBasicTerminalLaunchProperties(String name, File folder, File flixJarFile, List<String> arguments) {
		final Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "de.hetzge.eclipse.flix.processLauncherDelegate");
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, name);
		properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, null);
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, Utils.getJreExecutable().getAbsolutePath());
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, Stream.concat(Stream.of("-jar", flixJarFile.getAbsolutePath()), arguments.stream()).collect(Collectors.joining(" ")));
		if (folder != null) {
			properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, folder.getAbsolutePath());
		}
		return properties;
	}

	private static Done createDoneLogger(String key) {
		return (status) -> {
			LOG.log(new Status(status.getSeverity(), FlixConstants.PLUGIN_ID, key + ": " + status.getMessage(), status.getException()));
		};
	}
}
