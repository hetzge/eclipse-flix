package de.hetzge.eclipse.flix.internal;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

public final class FlixTextDocumentService implements TextDocumentService {

	private final FlixService flixService;

	public FlixTextDocumentService(FlixService flixService) {
		this.flixService = flixService;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		System.out.println("FlixLanguageServer.getTextDocumentService().new TextDocumentService() {...}.completion()");
		return this.flixService.complete(position).thenApply(completionList -> {
			return Either.forRight(completionList);
		});
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
		final List<TextDocumentContentChangeEvent> contentChangesEvents = params.getContentChanges();
		for (final TextDocumentContentChangeEvent contentChangeEvent : contentChangesEvents) {
			this.flixService.addUri(URI.create(params.getTextDocument().getUri()), contentChangeEvent.getText());
		}
	}
}