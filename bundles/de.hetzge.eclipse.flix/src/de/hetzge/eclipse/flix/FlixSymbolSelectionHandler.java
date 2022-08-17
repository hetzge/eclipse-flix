package de.hetzge.eclipse.flix;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.lxtk.WorkspaceSymbolProvider;
import org.lxtk.lx4e.ui.AbstractItemsSelectionHandler;
import org.lxtk.lx4e.ui.symbols.WorkspaceSymbolSelectionDialog;

/**
 * A handler that shows a dialog with a list of symbols to the user and opens
 * the selected symbol(s) in the corresponding editor(s).
 */
public class FlixSymbolSelectionHandler extends AbstractItemsSelectionHandler {
	@Override
	protected SelectionDialog createSelectionDialog(Shell shell, ExecutionEvent event) {
		final Iterator<WorkspaceSymbolProvider> iterator = Flix.get().getLanguageService().getWorkspaceSymbolProviders().iterator();
		if (!iterator.hasNext()) {
			return null;
		}

		final IResource resource = getResource(event);
		if (resource == null) {
			return null;
		}

		final List<WorkspaceSymbolProvider> providers = new ArrayList<>();
		final IProject project = resource.getProject();
		while (iterator.hasNext()) {
			final WorkspaceSymbolProvider provider = iterator.next();
			if (project.equals(provider.getContext())) {
				providers.add(provider);
			}
		}
		if (providers.isEmpty()) {
			return null;
		}

		final WorkspaceSymbolSelectionDialog dialog = new WorkspaceSymbolSelectionDialog(shell, providers.toArray(WorkspaceSymbolProvider[]::new), true);
		dialog.setTitle(MessageFormat.format("Open Symbol in ''{0}''", project.getName()));
		return dialog;
	}

	@Override
	protected Location getLocation(Object item) {
		return ((SymbolInformation) item).getLocation();
//		final Either<Location, WorkspaceSymbolLocation> location = ((WorkspaceSymbolItem) item).getResolvedLocation();
//
//		if (location.isLeft()) {
//			return location.getLeft();
//		} else {
//			return new Location(location.getRight().getUri(), new Range(new Position(0, 0), new Position(0, 0)));
//		}
	}

	private static IResource getResource(ExecutionEvent event) {
		final IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (part != null) {
			return Adapters.adapt(part.getEditorInput(), IResource.class);
		}

		final IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		return Adapters.adapt(selection.getFirstElement(), IResource.class);
	}
}
