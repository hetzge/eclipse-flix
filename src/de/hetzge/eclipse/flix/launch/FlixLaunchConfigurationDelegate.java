package de.hetzge.eclipse.flix.launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.ui.console.MessageConsole;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.utils.FlixUtils;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

// TODO JUnitViewEditorLauncher

public class FlixLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		System.out.println("FlixLaunchConfigurationDelegate.launch()");

		final FlixLaunchConfiguration launchConfiguration = new FlixLaunchConfiguration(configuration);
		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = FlixUtils.loadFlixJarFile();
		final IProject project = configuration.getMappedResources()[0].getProject();
		final MessageConsole console = EclipseUtils.console("Flix: " + configuration.getName());

		final List<String> command = new ArrayList<>();
		command.add(jreExecutableFile.getAbsolutePath());
		command.add("-jar");
		command.add(flixJarFile.getAbsolutePath());
		command.add("run");
		launchConfiguration.getEntrypoint().ifPresent(entrypoint -> {
			command.add("--entrypoint");
			command.add(entrypoint);
		});

		final ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(project.getLocation().toFile());
		processBuilder.redirectErrorStream(true);

		runInConsole(console, processBuilder);
	}

	private void runInConsole(final MessageConsole console, final ProcessBuilder processBuilder) throws CoreException {
		try (PrintWriter writer = new PrintWriter(console.newMessageStream())) {
			writer.write("<<COMMAND LINE>>\n");
			final long before = System.currentTimeMillis();
			final Process process = processBuilder.start();
			writer.write(process.info().commandLine().orElse(""));
			writer.write("\n");
			writer.write("<<OUTPUT>>\n");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				while (process.isAlive() || reader.ready()) {
					if (reader.ready()) {
						writer.println(reader.readLine());
						writer.flush();
					} else {
						Thread.sleep(10);
					}
				}
			}
			writer.write("<<TIME=" + (System.currentTimeMillis() - before) + "ms>>\n");
		} catch (final InterruptedException | IOException exception) {
			throw new CoreException(new Status(IStatus.ERROR, FlixConstants.PLUGIN_ID, "Failed", exception));
		}
	}
}
