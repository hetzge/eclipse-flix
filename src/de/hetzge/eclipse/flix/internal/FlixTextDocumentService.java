package de.hetzge.eclipse.flix.internal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

final class FlixTextDocumentService implements TextDocumentService {

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		System.out.println("FlixLanguageServer.getTextDocumentService().new TextDocumentService() {...}.completion()");
		return TextDocumentService.super.completion(position);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didSave()");
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didOpen()");
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didClose()");
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didChange()");
	}
}