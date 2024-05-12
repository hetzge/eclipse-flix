package de.hetzge.eclipse.flix;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import de.hetzge.eclipse.flix.launch.FlixLauncher;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixProjectBuilder extends IncrementalProjectBuilder {
	private static final ILog LOG = Platform.getLog(FlixProjectBuilder.class);

	private final FlixModel model;

	public FlixProjectBuilder() {
		this.model = Flix.get().getModel();
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		LOG.info("Run Flix project builder");
		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			final FlixProject flixProject = this.model.getFlixProjectOrThrowCoreException(getProject());
			final String projectName = flixProject.getProject().getName();
			monitor.subTask(String.format("Stop language tooling (%s)", projectName));
			Flix.get().getLanguageToolingManager().disconnectProject(flixProject);
			monitor.subTask(String.format("Delete temporary folders (%s)", projectName));
			flixProject.deleteTemporaryFolders(monitor);
			monitor.subTask(String.format("Build flix project (%s)", projectName));
			try {
				FlixLauncher.launchBuild(flixProject).waitFor();
			} catch (final InterruptedException exception) {
				throw new RuntimeException(exception);
			}
			monitor.subTask(String.format("Refresh project (%s)", projectName));
			flixProject.getProject().refreshLocal(IProject.DEPTH_INFINITE, monitor);
			monitor.subTask(String.format("Start language tooling (%s)", projectName));
			Flix.get().getLanguageToolingManager().connectProject(flixProject);
		}
		return null;
	}

}
