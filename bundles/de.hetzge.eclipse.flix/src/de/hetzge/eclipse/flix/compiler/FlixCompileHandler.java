package de.hetzge.eclipse.flix.compiler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixCompileHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Flix.get().getLanguageToolingManager().compile(getFlixProject());
		return null;
	}

	private FlixProject getFlixProject() throws ExecutionException {
		final ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			final Object item = ((IStructuredSelection) selection).getFirstElement();
			final IProject project = Adapters.adapt(item, IProject.class);
			return Flix.get().getModel().getOrCreateFlixProject(project).orElseThrow(() -> new ExecutionException("Not a valid Flix project: " + project.getName())); //$NON-NLS-1$
		} else {
			throw new ExecutionException("No project found"); //$NON-NLS-1$
		}
	}

}
