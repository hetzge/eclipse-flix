package de.hetzge.eclipse.flix.editor.outline;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.lsp4j.DocumentSymbol;

public final class FlixContentOutlineProvider implements ITreeContentProvider {

	private List<DocumentSymbol> rootSymbols;

	public void setRootSymbols(List<DocumentSymbol> rootSymbols) {
		this.rootSymbols = rootSymbols;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (this.rootSymbols == null) {
			return new Object[] {};
		}
		return this.rootSymbols.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof DocumentSymbol)) {
			return new Object[] {};
		}
		final DocumentSymbol documentSymbol = (DocumentSymbol) parentElement;
		return documentSymbol.getChildren().toArray();
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof DocumentSymbol)) {
			return false;
		}
		final DocumentSymbol documentSymbol = (DocumentSymbol) element;
		return !documentSymbol.getChildren().isEmpty();
	}

}
