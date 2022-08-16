package de.hetzge.eclipse.flix.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.IElementImplExtension;
import org.eclipse.handly.model.impl.support.Body;
import org.eclipse.handly.model.impl.support.Element;

import de.hetzge.eclipse.flix.Activator;

public class FlixDeltaProcessor implements IResourceDeltaVisitor {

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		switch (delta.getResource().getType()) {
		case IResource.ROOT:
			return true;
		case IResource.PROJECT:
			final IProject project = (IProject) delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				return processAddedProject(project);
			case IResourceDelta.REMOVED:
				return processRemovedProject(project);
			default:
				return true;
			}
		default:
			return true;
		}
	}

	private boolean processAddedProject(IProject project) {
		System.out.println("FlixDeltaProcessor.processAddedProject()");
		final FlixModel flixModel = Activator.getDefault().getModelManager().getModel();
		getBody(flixModel).addChild(new FlixProject(flixModel, project));
		return false;
	}

	private boolean processRemovedProject(IProject project) {
		System.out.println("FlixDeltaProcessor.processRemovedProject()");
		final FlixModel flixModel = Activator.getDefault().getModelManager().getModel();
		getBody(flixModel).removeChild(new FlixProject(flixModel, project));
		close(flixModel);
		return false;
	}

	private Body getBody(IElementImplExtension element) {
		try {
			return (Body) element.getBody_();
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static void close(IElement element) {
		((Element) element).close_();
	}
}
