package de.hetzge.eclipse.flix.compiler;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;

import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixLanguageServerLaunchConfigurationDelegate extends JavaLaunchDelegate {

	private static final String ID = "de.hetzge.eclipse.flix.lspLaunchConfigurationType";

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.launch(configuration, mode, launch, monitor);
	}

	public static FlixLanguageServerLaunch launch(FlixProject project, int compilerPort) {
		System.out.println("FlixLanguageServerLaunchConfigurationDelegate.launch()");
		try {
			final IRuntimeClasspathEntry entry = JavaRuntime.newArchiveRuntimeClasspathEntry(Path.fromOSString(project.getFlixCompilerJarFile().getAbsolutePath()));
			entry.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
			final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			final ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(ID);
			final ILaunchConfigurationWorkingCopy configurationWorkingCopy = launchConfigurationType.newInstance(null, String.format("Flix LSP Server (%s)", project.getProject().getName()));
			configurationWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, project.getProject().getLocation().toFile().getAbsolutePath());
			configurationWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "ca.uwaterloo.flix.Main");
			configurationWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
			configurationWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, List.of(entry.getMemento()));
			configurationWorkingCopy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
			configurationWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "lsp " + compilerPort);
			configurationWorkingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-XX:+UseG1GC -XX:+UseStringDeduplication -Xss4m -Xms100m -Xmx2G");
			final ILaunch launch = configurationWorkingCopy.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
			return new FlixLanguageServerLaunch(launch);
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}
}
