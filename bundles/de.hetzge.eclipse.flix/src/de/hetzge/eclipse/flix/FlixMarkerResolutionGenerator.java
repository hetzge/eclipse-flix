package de.hetzge.eclipse.flix;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.codeaction.AbstractMarkerResolutionGenerator;

/**
 * Flix-specific extension of {@link AbstractMarkerResolutionGenerator}.
 */
public class FlixMarkerResolutionGenerator extends AbstractMarkerResolutionGenerator {
	public static final String MARKER_TYPE = "de.hetzge.eclipse.flix.problem"; //$NON-NLS-1$

	@Override
	protected LanguageOperationTarget getLanguageOperationTarget(IMarker marker) {
		final IResource resource = marker.getResource();
		if (!(resource instanceof IFile)) {
			return null;
		}
		if (!MarkerUtilities.isMarkerType(marker, MARKER_TYPE)) {
			return null;
		}
		return FlixOperationTargetProvider.getOperationTarget((IFile) resource);
	}

	@Override
	protected IWorkspaceEditChangeFactory getWorkspaceEditChangeFactory() {
		return Flix.get().getChangeFactory();
	}
}
