package de.hetzge.eclipse.flix.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IElementDelta;
import org.eclipse.handly.model.IElementDeltaConstants;
import org.eclipse.handly.model.impl.support.Element;
import org.eclipse.handly.model.impl.support.ElementDelta;
import org.lxtk.lx4e.model.ILanguageElement;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.impl.LanguageElementDelta;

import de.hetzge.eclipse.flix.Flix;

class FlixDeltaProcessor implements IResourceDeltaVisitor {

	private final List<IElementDelta> deltas;
	private final FlixModel model;
	private final List<IFlixProject> flixProjectsBefore;

	public FlixDeltaProcessor() {
		this.deltas = new ArrayList<>();
		this.model = Flix.get().getModelManager().getModel();
		this.flixProjectsBefore = this.model.getFlixProjects();
	}

	public IElementDelta[] getDeltas() {
		return this.deltas.toArray(new IElementDelta[this.deltas.size()]);
	}

	@Override
	public boolean visit(IResourceDelta resourceDelta) throws CoreException {
		switch (resourceDelta.getResource().getType()) {
		case IResource.ROOT:
			return true;
		case IResource.PROJECT:
			final IProject project = (IProject) resourceDelta.getResource();
			switch (resourceDelta.getKind()) {
			case IResourceDelta.ADDED:
				return processAddedProject(project, 0L);
			case IResourceDelta.REMOVED:
				return processRemovedProject(project, 0L);
			case IResourceDelta.CHANGED:
				final boolean isOpenDelta = (resourceDelta.getFlags() & IResourceDelta.OPEN) != 0;
				final boolean isDescriptionDelta = (resourceDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0;
				return processChangedProject(project, isOpenDelta, isDescriptionDelta);
			default:
				return true;
			}
		case IResource.FILE:
			switch (resourceDelta.getKind()) {
			case IResourceDelta.ADDED:
				return false; // TODO
			case IResourceDelta.REMOVED:
				return false; // TODO
			case IResourceDelta.CHANGED:
				return processChangedFile(resourceDelta);
			default:
				return false;
			}
		default:
			return true;
		}
	}

	private boolean processAddedProject(IProject project, long flags) {
		System.out.println("FlixDeltaProcessor.processAddedProject()");
		if (isFlixProject(project)) {
			final FlixProject flixProject = this.model.addFlixProject(project);
			this.deltas.add(createDeltaBuilder().added(flixProject, flags).getDelta());
		}
		return false;
	}

	private boolean processRemovedProject(IProject project, long flags) {
		System.out.println("FlixDeltaProcessor.processRemovedProject()");
		if (wasFlixProject(project)) {
			final FlixProject flixProject = this.model.removeFlixProject(project);
			this.deltas.add(createDeltaBuilder().removed(flixProject, flags).getDelta());
		}
		return false;
	}

	private boolean processChangedProject(IProject project, boolean isOpenDelta, boolean isDescriptionDelta) {
		System.out.println("FlixDeltaProcessor.processChangedProject()");

		if (isOpenDelta) {
			if (project.isOpen()) {
				return processAddedProject(project, IElementDeltaConstants.F_OPEN);
			} else {
				return processRemovedProject(project, IElementDeltaConstants.F_OPEN);
			}
		}

		if (isDescriptionDelta) {
			final boolean isFlixProject = isFlixProject(project);
			final boolean wasFooProject = wasFlixProject(project);
			if (wasFooProject != isFlixProject) {
				if (isFlixProject) {
					return processAddedProject(project, IElementDeltaConstants.F_DESCRIPTION);
				} else {
					return processRemovedProject(project, IElementDeltaConstants.F_DESCRIPTION);
				}
			}
		}

		// On change process children to update outline ...
		return true;
	}

	private boolean processChangedFile(IResourceDelta resourceDelta) {
		System.out.println("FlixDeltaProcessor.processChangedFile() " + resourceDelta);

		final IFile file = (IFile) resourceDelta.getResource();
		final ILanguageElement element = new FlixSourceFile(file.getLocationURI());
		if (element != null) {
			final LanguageElementDelta elementDelta = new LanguageElementDelta(element);
			elementDelta.setKind(IElementDeltaConstants.CHANGED);

			long flags = 0;

			final boolean isWorkingCopy = isWorkingCopy(element);

			if (isWorkingCopy) {
				flags |= IElementDeltaConstants.F_UNDERLYING_RESOURCE;
			}

			if ((resourceDelta.getFlags() & ~(IResourceDelta.MARKERS | IResourceDelta.SYNC)) != 0) {
				flags |= IElementDeltaConstants.F_CONTENT;
				if (!isWorkingCopy) {
					close(element);
				}
			}

			if ((resourceDelta.getFlags() & IResourceDelta.MARKERS) != 0) {
				flags |= IElementDeltaConstants.F_MARKERS;
				elementDelta.setMarkerDeltas(resourceDelta.getMarkerDeltas());
			}

			if ((resourceDelta.getFlags() & IResourceDelta.SYNC) != 0) {
				flags |= IElementDeltaConstants.F_SYNC;
			}

			elementDelta.setFlags(flags);

			this.deltas.add(elementDelta);
		}
		return false;
	}

	private ElementDelta.Builder createDeltaBuilder() {
		return new ElementDelta.Builder(new ElementDelta(this.model));
	}

	private static boolean isWorkingCopy(ILanguageElement element) {
		return element instanceof ILanguageSourceFile && ((ILanguageSourceFile) element).isWorkingCopy();
	}

	private static void close(IElement element) {
		((Element) element).close_();
	}

	private boolean wasFlixProject(IProject project) {
		return this.flixProjectsBefore.stream().filter(flixProject -> flixProject.getProject().equals(project)).findFirst().isPresent();
	}

	private boolean isFlixProject(IProject project) {
		return FlixProject.isActiveFlixProject(project);
	}
}
