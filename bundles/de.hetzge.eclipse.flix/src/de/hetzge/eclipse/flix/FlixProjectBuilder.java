package de.hetzge.eclipse.flix;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import de.hetzge.eclipse.flix.launch.FlixLauncher;
import de.hetzge.eclipse.flix.model.api.IFlixModel;
import de.hetzge.eclipse.flix.model.api.IFlixProject;

public class FlixProjectBuilder extends IncrementalProjectBuilder {

	private final IFlixModel model;

	public FlixProjectBuilder() {
		this.model = Flix.get().getModelManager().getModel();
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		System.out.println("IncrementalProjectBuilder1.build(" + kind + ")");

		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			final IFlixProject flixProject = this.model.getFlixProject(getProject()).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));
			monitor.subTask("Delete build folder");
			flixProject.deleteBuildFolder(monitor);
			monitor.subTask("Restart language server");
			flixProject.restart();
			monitor.subTask("Build flix project");
			FlixLauncher.launchBuild(flixProject);
		}

		return null;
	}

}
