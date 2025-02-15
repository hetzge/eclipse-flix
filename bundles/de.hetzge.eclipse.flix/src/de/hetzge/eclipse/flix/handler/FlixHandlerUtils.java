package de.hetzge.eclipse.flix.handler;

import java.util.Optional;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.utils.EclipseUtils;

final class FlixHandlerUtils {

	private FlixHandlerUtils() {
	}

	public static FlixProject getFlixProject() throws ExecutionException {
		final ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			final Object item = ((IStructuredSelection) selection).getFirstElement();
			final IProject project = Adapters.adapt(item, IProject.class);
			return Flix.get().getModel().getFlixProjectOrThrowExecutionException(project);
		}
		final Optional<FlixProject> projectOptional = EclipseUtils.activeResource()
				.map(IResource::getProject)
				.flatMap(Flix.get().getModel()::getFlixProject);
		if (projectOptional.isPresent()) {
			return projectOptional.get();
		}
		throw new ExecutionException("No project found"); //$NON-NLS-1$
	}
}
