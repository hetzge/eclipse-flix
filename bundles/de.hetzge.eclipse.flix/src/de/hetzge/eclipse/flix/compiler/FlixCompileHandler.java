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
import de.hetzge.eclipse.flix.FlixLanguageToolingManager;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixCompileHandler extends AbstractHandler {

	private final FlixLanguageToolingManager languageToolingManager;
	private final FlixModel model;

	public FlixCompileHandler() {
		this.languageToolingManager = Flix.get().getLanguageToolingManager();
		this.model = Flix.get().getModel();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("FlixCompileHandler.execute()");
		Flix.get().getLanguageToolingManager().compile(getFlixProject());
		return null;
	}

	private FlixProject getFlixProject() throws ExecutionException {
		final ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			final Object item = ((IStructuredSelection) selection).getFirstElement();
			final IProject project = Adapters.adapt(item, IProject.class);
			return this.model.getFlixProject(project).orElseThrow(() -> new ExecutionException("Not a valid flix project"));
		} else {
			throw new ExecutionException("No project found");
		}
	}

}
