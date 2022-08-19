package de.hetzge.eclipse.flix.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.utils.Utils;

//TODO JUnitViewEditorLauncher

public class FlixLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigurationDelegate.class);

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		LOG.info(String.format("Launch '%s'", configuration.getName()));

		final IProject project = configuration.getMappedResources()[0].getProject();
		final IFlixProject flixProject = Flix.get().getModelManager().getModel().getFlixProject(project).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));

		final FlixLaunchConfiguration launchConfiguration = new FlixLaunchConfiguration(configuration);
		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = flixProject.getFlixCompilerJarFile();

		final List<String> command = new ArrayList<>();
		command.add(jreExecutableFile.getAbsolutePath());
		command.add("-jar");
		command.add(flixJarFile.getAbsolutePath());
		command.add("run");
		launchConfiguration.getEntrypoint().ifPresent(entrypoint -> {
			command.add("--entrypoint");
			command.add(entrypoint);
		});
		for (final IFile sourceFile : flixProject.getFlixSourceFiles()) {
			command.add(sourceFile.getFullPath().toFile().getAbsolutePath());
		}
		for (final IFile libraryFile : flixProject.getFlixFpkgLibraryFiles()) {
			command.add(libraryFile.getFullPath().toFile().getAbsolutePath());
		}
		for (final IFile libraryFile : flixProject.getFlixJarLibraryFiles()) {
			command.add(libraryFile.getFullPath().toFile().getAbsolutePath());
		}

		final ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(project.getLocation().toFile());
		processBuilder.redirectErrorStream(true);

		try {
			final Process process = processBuilder.start();
			LOG.info("Execute: " + process.info().commandLine().orElse(""));
			// https://wiki.eclipse.org/Eclipse_Plug-in_Development_FAQ#How_do_I_capture_the_output_of_my_launched_application_like_the_.27Console.27_view.3F
			DebugPlugin.newProcess(launch, processBuilder.start(), configuration.getName());
		} catch (final IOException exception) {
			throw new CoreException(new Status(IStatus.ERROR, FlixConstants.PLUGIN_ID, "Failed", exception));
		}
	}

}
