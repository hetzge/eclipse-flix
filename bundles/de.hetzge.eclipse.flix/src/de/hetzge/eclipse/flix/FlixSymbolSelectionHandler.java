package de.hetzge.eclipse.flix;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
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
			LOG.warn("Skip selection dialog because no workspace symbol provider found"); //$NON-NLS-1$
			return null;
		}

		final WorkspaceSymbolSelectionDialog dialog = new FlixWorkspaceSymbolSelectionDialog(shell, providers.toArray(WorkspaceSymbolProvider[]::new));
		dialog.setTitle("Open symbol in workspace");

		final Optional<String> selectionTextOptional = EclipseUtils.getActiveEditorSelectionText();
		if (selectionTextOptional.isPresent()) {
			final String selectionText = selectionTextOptional.get();
			dialog.setInitialPattern(selectionText, FilteredItemsSelectionDialog.FULL_SELECTION);
		}
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

	private static class FlixWorkspaceSymbolSelectionDialog extends WorkspaceSymbolSelectionDialog {

		public FlixWorkspaceSymbolSelectionDialog(Shell shell, WorkspaceSymbolProvider[] array) {
			super(shell, array, true);
		}

		@Override
		protected Object newWorkspaceSymbolItem(Either<SymbolInformation, WorkspaceSymbol> symbol, WorkspaceSymbolProvider workspaceSymbolProvider) {
			final FlixWorkspaceSymbolItem workspaceSymbolItem = new FlixWorkspaceSymbolItem(symbol);
			workspaceSymbolItem.setWorkspaceSymbolProvider(workspaceSymbolProvider);
			return workspaceSymbolItem;
		}
	}

	private static class FlixWorkspaceSymbolItem extends WorkspaceSymbolItem {

		private final Either<SymbolInformation, WorkspaceSymbol> symbol;

		public FlixWorkspaceSymbolItem(Either<SymbolInformation, WorkspaceSymbol> symbol) {
			super(symbol);
			this.symbol = symbol;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.symbol);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final FlixWorkspaceSymbolItem other = (FlixWorkspaceSymbolItem) obj;
			return Objects.equals(this.symbol, other.symbol);
		}
	}
}
