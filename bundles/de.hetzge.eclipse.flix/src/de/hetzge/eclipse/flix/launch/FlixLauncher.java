package de.hetzge.eclipse.flix.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService.Done;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.core.model.FlixVersion;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.utils.FlixUtils;

public final class FlixLauncher {

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigurationDelegate.class);
	private static final Map<String, Map<String, Object>> PROPERTIES_BY_KEY = new ConcurrentHashMap<>();

	private FlixLauncher() {
	}

	public static Process launchRun(FlixLaunchConfiguration launchConfiguration, FlixProject flixProject) {
		final String name = "Run " + flixProject.getProject().getName();
		final List<String> arguments = new ArrayList<>();
		arguments.add("run");
		launchConfiguration.getEntrypoint().ifPresent(entrypoint -> {
			arguments.add("--entrypoint");
			arguments.add(entrypoint);
		});
		launchConfiguration.getArguments().ifPresent(launchArguments -> {
			arguments.add("--args");
			arguments.add(String.format("'%s'", launchArguments));
		});
		final IVMInstall vmInstall = launchConfiguration.getVmInstall();
		final File folder = flixProject.getProject().getLocation() != null ? flixProject.getProject().getLocation().toFile() : null;
		final File flixJarFile = flixProject.getFlixCompilerJarFile();
		final Process process = startProcess(folder, vmInstall, flixJarFile, arguments);
		openTerminal(createBasicTerminalLaunchProperties(name, process), String.format("RUN_%s", flixProject.getProject().getFullPath().toOSString()));
		return process;
	}

	public static Process launchRepl(FlixProject flixProject) {
		final String name = "Repl " + flixProject.getProject().getName();
		final IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		final File folder = flixProject.getProject().getLocation() != null ? flixProject.getProject().getLocation().toFile() : null;
		final File flixJarFile = flixProject.getFlixCompilerJarFile();
		final Process process = startProcess(folder, vmInstall, flixJarFile, List.of());
		openTerminal(createBasicTerminalLaunchProperties(name, process), String.format("REPL_%s", flixProject.getProject().getFullPath().toOSString()));
		return process;
	}

	public static Process launchTest(FlixLaunchConfiguration launchConfiguration, FlixProject flixProject) {
		final String name = "Test " + flixProject.getProject().getName();
		final IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		final File folder = flixProject.getProject().getLocation() != null ? flixProject.getProject().getLocation().toFile() : null;
		final File flixJarFile = flixProject.getFlixCompilerJarFile();
		final Process process = startProcess(folder, vmInstall, flixJarFile, List.of("test"));
		openTerminal(createBasicTerminalLaunchProperties(name, process), String.format("TEST_%s", flixProject.getProject().getFullPath().toOSString()));
		return process;
	}

	public static Process launchBuild(FlixProject flixProject) {
		final String name = "Build " + flixProject.getProject().getName();
		final IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		final File folder = flixProject.getProject().getLocation() != null ? flixProject.getProject().getLocation().toFile() : null;
		final File flixJarFile = flixProject.getFlixCompilerJarFile();
		final Process process = startProcess(folder, vmInstall, flixJarFile, List.of("build"));
		openTerminal(createBasicTerminalLaunchProperties(name, process), String.format("BUILD_%s", flixProject.getProject().getFullPath().toOSString()));
		return process;
	}

	public static Process launchInit(File folder, FlixVersion flixVersion) {
		final String name = "Init " + folder.getName();
		final IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		final File flixJarFile = FlixUtils.loadFlixJarFile(flixVersion, null);
		final Process process = startProcess(folder, vmInstall, flixJarFile, List.of("init"));
		openTerminal(createBasicTerminalLaunchProperties(name, process), String.format("INIT_%s", folder.getAbsolutePath()));
		return process;
	}

	private static void openTerminal(Map<String, Object> properties, String launchKey) {
		final Map<String, Object> lastProperties = PROPERTIES_BY_KEY.getOrDefault(launchKey, properties);
		final ITerminalService terminalService = TerminalServiceFactory.getService();
		terminalService.terminateConsole(lastProperties, createDoneLogger("terminateConsole"));
		terminalService.closeConsole(lastProperties, createDoneLogger("closeConsole"));
		terminalService.openConsole(properties, createDoneLogger("openConsole"));
		PROPERTIES_BY_KEY.put(launchKey, properties);
	}

	private static Map<String, Object> createBasicTerminalLaunchProperties(String name, Process process) {
		final Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, "de.hetzge.eclipse.flix.processLauncherDelegate");
		properties.put(ITerminalsConnectorConstants.PROP_TITLE, name);
		properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, null);
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_OBJ, process);
		return properties;
	}

	private static Process startProcess(File folder, IVMInstall vmInstall, File flixJarFile, List<String> arguments) {
		final List<String> commands = new ArrayList<>();
		commands.add(new File(vmInstall.getInstallLocation(), "bin/java").getAbsolutePath());
		commands.add("-jar");
		commands.add(flixJarFile.getAbsolutePath());
		commands.addAll(arguments);
		final ProcessBuilder processBuilder = new ProcessBuilder(commands);
		processBuilder.directory(folder);
		final Process process;
		try {
			process = processBuilder.start();
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
		return process;
	}

	private static Done createDoneLogger(String key) {
		return (status) -> {
			LOG.log(new Status(status.getSeverity(), FlixConstants.PLUGIN_ID, key + ": " + status.getMessage(), status.getException()));
		};
	}
}
