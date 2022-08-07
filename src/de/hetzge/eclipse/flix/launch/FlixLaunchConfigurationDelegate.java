package de.hetzge.eclipse.flix.launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import de.hetzge.eclipse.flix.Activator;
import de.hetzge.eclipse.flix.FlixUtils;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

public class FlixLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		System.out.println("FlixLaunchConfigurationDelegate.launch()");

		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = FlixUtils.loadFlixJarFile();
		final IProject project = configuration.getMappedResources()[0].getProject();
		final List<IFile> flixFiles = FlixUtils.findFlixFiles(project);
		final MessageConsole console = EclipseUtils.console("Flix: " + configuration.getName());
		final MessageConsoleStream messageConsoleStream = console.newMessageStream();

		final List<String> command = new ArrayList<>();
		command.add(jreExecutableFile.getAbsolutePath());
		command.add("-jar");
		command.add(flixJarFile.getAbsolutePath());
		command.add("run");
		for (final IFile flixFile : flixFiles) {
			command.add(flixFile.getFullPath().toFile().getAbsolutePath());
		}

		final ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(project.getLocation().toFile());
		processBuilder.redirectErrorStream(true);
		try {
			try (PrintWriter writer = new PrintWriter(messageConsoleStream)) {
				writer.write("### RUN ###\n");
				final Process process = processBuilder.start();
				writer.write(process.info().commandLine().orElse(""));
				writer.write("\n");
				writer.write("###########\n");
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
				writer.write("### END ###\n");
			}
		} catch (final InterruptedException | IOException exception) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed", exception));
		}
	}
}
