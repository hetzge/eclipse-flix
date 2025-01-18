package de.hetzge.eclipse.flix.project;

import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixModelFactory;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixProjectNature implements IProjectNature {

	public static final String ID = "de.hetzge.eclipse.flix.nature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		System.out.println("FlixProjectNature.configure()");
		EclipseUtils.addBuilder(this.project, FlixConstants.FLIX_BUILDER_ID);
		addToModel(this.project);
	}

	@Override
	public void deconfigure() throws CoreException {
		System.out.println("FlixProjectNature.deconfigure()");
		EclipseUtils.removeBuilder(this.project, FlixConstants.FLIX_BUILDER_ID);
		removeFromModel(this.project);
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	private static void addToModel(IProject project) {
		final FlixProject flixProject = FlixModelFactory.createFlixProject(project);
		Flix.get().getModel().addFlixProject(flixProject);
		Flix.get().getLanguageToolingManager().reconnectProject(flixProject);
	}

	private static void removeFromModel(IProject project) {
		final Flix flix = Flix.get();
		final FlixModel flixModel = flix.getModel();
		final Optional<FlixProject> optionalFlixProject = flixModel.getFlixProject(project);
		if (optionalFlixProject.isPresent()) {
			final FlixProject flixProject = optionalFlixProject.get();
			flix.getLanguageToolingManager().disconnectProject(flixProject);
			flixModel.removeProject(flixProject);
		}
	}
}
