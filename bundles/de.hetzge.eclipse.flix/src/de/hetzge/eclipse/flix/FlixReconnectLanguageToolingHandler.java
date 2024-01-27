package de.hetzge.eclipse.flix;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixReconnectLanguageToolingHandler extends AbstractHandler {

	private final FlixLanguageToolingManager languageToolingManager;
	private final FlixModel model;

	public FlixReconnectLanguageToolingHandler() {
		this.languageToolingManager = Flix.get().getLanguageToolingManager();
		this.model = Flix.get().getModel();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		this.languageToolingManager.reconnectProject(getFlixProject());
		return null;
	}

	private FlixProject getFlixProject() throws ExecutionException {
		final ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			final Object item = ((IStructuredSelection) selection).getFirstElement();
			final IProject project = Adapters.adapt(item, IProject.class);
			return this.model.getFlixProject(project).orElseThrow(() -> new ExecutionException("Not a valid flix project")); //$NON-NLS-1$
		} else {
			throw new ExecutionException("No project found"); //$NON-NLS-1$
		}
	}

}
