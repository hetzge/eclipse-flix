package de.hetzge.eclipse.flix.model;

import static org.eclipse.handly.model.IElementDeltaConstants.CHANGED;
import static org.eclipse.handly.model.IElementDeltaConstants.F_CONTENT;
import static org.eclipse.handly.model.IElementDeltaConstants.F_MARKERS;
import static org.eclipse.handly.model.IElementDeltaConstants.F_SYNC;
import static org.eclipse.handly.model.IElementDeltaConstants.F_UNDERLYING_RESOURCE;

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
import org.eclipse.handly.model.impl.IElementImplExtension;
import org.eclipse.handly.model.impl.support.Body;
import org.eclipse.handly.model.impl.support.Element;
import org.lxtk.lx4e.model.ILanguageElement;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.impl.LanguageElementDelta;

import de.hetzge.eclipse.flix.Flix;

public class FlixDeltaProcessor implements IResourceDeltaVisitor {

	private final List<IElementDelta> deltas;

	public FlixDeltaProcessor() {
		this.deltas = new ArrayList<>();
	}

	public IElementDelta[] getDeltas() {
		return this.deltas.toArray(new IElementDelta[this.deltas.size()]);
	}

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
		case IResource.FILE:
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				return false; // TODO
			case IResourceDelta.REMOVED:
				return false; // TODO
			case IResourceDelta.CHANGED:
				return processChangedFile(delta);
			default:
				return false;
			}
		default:
			return true;
		}
	}

	private boolean processAddedProject(IProject project) {
		System.out.println("FlixDeltaProcessor.processAddedProject()");
		final FlixModel flixModel = Flix.get().getModelManager().getModel();
		getBody(flixModel).addChild(new FlixProject(flixModel, project));
		return false;
	}

	private boolean processRemovedProject(IProject project) {
		System.out.println("FlixDeltaProcessor.processRemovedProject()");
		final FlixModel flixModel = Flix.get().getModelManager().getModel();
		getBody(flixModel).removeChild(new FlixProject(flixModel, project));
		close(flixModel);
		return false;
	}

	private boolean processChangedFile(IResourceDelta delta) {
		System.out.println("FlixDeltaProcessor.processChangedFile()");

		final IFile file = (IFile) delta.getResource();
		final ILanguageElement element = new FlixSourceFile(file.getLocationURI());
		if (element != null) {
			final LanguageElementDelta result = new LanguageElementDelta(element);
			result.setKind(CHANGED);

			long flags = 0;

			final boolean isWorkingCopy = isWorkingCopy(element);

			if (isWorkingCopy) {
				flags |= F_UNDERLYING_RESOURCE;
			}

			if ((delta.getFlags() & ~(IResourceDelta.MARKERS | IResourceDelta.SYNC)) != 0) {
				flags |= F_CONTENT;
				if (!isWorkingCopy) {
					close(element);
				}
			}

			if ((delta.getFlags() & IResourceDelta.MARKERS) != 0) {
				flags |= F_MARKERS;
				result.setMarkerDeltas(delta.getMarkerDeltas());
			}

			if ((delta.getFlags() & IResourceDelta.SYNC) != 0) {
				flags |= F_SYNC;
			}

			result.setFlags(flags);

			this.deltas.add(result);
		}
		return false;
	}

	private static boolean isWorkingCopy(ILanguageElement element) {
		return element instanceof ILanguageSourceFile && ((ILanguageSourceFile) element).isWorkingCopy();
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
