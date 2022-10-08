package de.hetzge.eclipse.flix.server;

import org.eclipse.lsp4j.DidChangeNotebookDocumentParams;
import org.eclipse.lsp4j.DidCloseNotebookDocumentParams;
import org.eclipse.lsp4j.DidOpenNotebookDocumentParams;
import org.eclipse.lsp4j.DidSaveNotebookDocumentParams;
import org.eclipse.lsp4j.services.NotebookDocumentService;

public final class FlixNotebookDocumentService implements NotebookDocumentService {
	@Override
	public void didSave(DidSaveNotebookDocumentParams params) {
	}

	@Override
	public void didOpen(DidOpenNotebookDocumentParams params) {
	}

	@Override
	public void didClose(DidCloseNotebookDocumentParams params) {
	}

	@Override
	public void didChange(DidChangeNotebookDocumentParams params) {
	}
}