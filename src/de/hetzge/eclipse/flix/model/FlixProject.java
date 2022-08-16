package de.hetzge.eclipse.flix.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.impl.support.Element;
import org.eclipse.handly.model.impl.support.IModelManager;

import de.hetzge.eclipse.flix.Activator;

public class FlixProject extends Element implements IFlixProject {

	private final IProject project;

	public FlixProject(FlixModel parent, IProject project) {
		super(parent, project.getName());
		this.project = project;
	}

	@Override
	public void validateExistence_(IContext context) throws CoreException {
	}

	@Override
	public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public IModelManager getModelManager_() {
		return Activator.getDefault().getModelManager();
	}

	@Override
	public IResource getResource_() {
		return this.project;
	}
}
