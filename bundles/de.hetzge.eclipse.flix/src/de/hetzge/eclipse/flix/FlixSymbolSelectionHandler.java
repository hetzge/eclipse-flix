package de.hetzge.eclipse.flix;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceSymbolProvider;
import org.lxtk.lx4e.ui.AbstractItemsSelectionHandler;
import org.lxtk.lx4e.ui.symbols.WorkspaceSymbolItem;
import org.lxtk.lx4e.ui.symbols.WorkspaceSymbolSelectionDialog;

import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

/**
 * A handler that shows a dialog with a list of symbols to the user and opens
 * the selected symbol(s) in the corresponding editor(s).
 */
public class FlixSymbolSelectionHandler extends AbstractItemsSelectionHandler {

	private static final ILog LOG = Platform.getLog(EclipseUtils.class);

	private final LanguageService languageService;

	public FlixSymbolSelectionHandler() {
		this.languageService = Flix.get().getLanguageService();
	}

	@Override
	protected SelectionDialog createSelectionDialog(Shell shell, ExecutionEvent event) {
		final List<WorkspaceSymbolProvider> providers = Utils.toList(this.languageService.getWorkspaceSymbolProviders());
		if (providers.isEmpty()) {
			LOG.warn("Skip selection dialog because no workspace symbol provider found");
			return null;
		}

		final WorkspaceSymbolSelectionDialog dialog = new WorkspaceSymbolSelectionDialog(shell, providers.toArray(WorkspaceSymbolProvider[]::new), true);
		dialog.setTitle("Open symbol in workspace");
		return dialog;
	}

	@Override
	protected Location getLocation(Object item) {
		final Either<Location, WorkspaceSymbolLocation> location = ((WorkspaceSymbolItem) item).getResolvedLocation();

		if (location.isLeft()) {
			return location.getLeft();
		} else {
			return new Location(location.getRight().getUri(), new Range(new Position(0, 0), new Position(0, 0)));
		}
	}
}
