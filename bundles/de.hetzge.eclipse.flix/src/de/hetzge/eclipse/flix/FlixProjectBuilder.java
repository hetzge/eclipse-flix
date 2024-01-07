package de.hetzge.eclipse.flix;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hetzge.eclipse.flix.launch.FlixLauncher;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixProjectBuilder extends IncrementalProjectBuilder {

	private final FlixModel model;

	public FlixProjectBuilder() {
		this.model = Flix.get().getModel();
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		System.out.println("FlixProjectBuilder.build(" + kind + ")");
		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			final FlixProject flixProject = this.model.getFlixProjectOrThrowCoreException(getProject());
			monitor.subTask("Delete build folder");
			flixProject.deleteBuildFolder(monitor);
			monitor.subTask("Build flix project");
			FlixLauncher.launchBuild(flixProject);
		}
		return null;
	}

}
