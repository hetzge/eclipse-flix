package de.hetzge.eclipse.flix.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.impl.support.Element;
import org.eclipse.handly.model.impl.support.IModelManager;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.project.FlixProjectNature;

public class FlixProject extends Element implements IFlixProject {

	private final IProject project;

	public FlixProject(FlixModel parent, IProject project) {
		super(parent, project.getName());
		this.project = project;
	}

	@Override
	public void validateExistence_(IContext context) throws CoreException {
		if (!isActive()) {
			throw newDoesNotExistException_();
		}
	}

	@Override
	public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public IModelManager getModelManager_() {
		return Flix.get().getModelManager();
	}

	@Override
	public IResource getResource_() {
		return this.project;
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public boolean isActive() {
		return isActiveFlixProject(this.project);
	}

	public static boolean isActiveFlixProject(IProject project) {
		return SafeRunner.run(() -> project.isOpen() && project.getDescription().hasNature(FlixProjectNature.ID));
	}
}
