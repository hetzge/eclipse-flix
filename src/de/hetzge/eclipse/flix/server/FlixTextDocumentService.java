package de.hetzge.eclipse.flix.server;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import de.hetzge.eclipse.flix.GsonUtils;

public final class FlixTextDocumentService implements TextDocumentService {

	private final FlixServerService flixService;

	public FlixTextDocumentService(FlixServerService flixService) {
		this.flixService = flixService;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		System.out.println("FlixTextDocumentService.completion()");
		return this.flixService.complete(position).thenApply(completionList -> {
			return Either.forRight(completionList);
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declaration(DeclarationParams params) {
		System.out.println("FlixTextDocumentService.declaration()");
		return this.flixService.decleration(params).thenApply(completionList -> {
			System.out.println("Declaration: " + GsonUtils.getGson().toJson(completionList));
			return Either.forRight(completionList);
		});
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		System.out.println("FlixTextDocumentService.hover()");
		return this.flixService.hover(params);
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
		System.out.println("FlixTextDocumentService.documentSymbol()");
		return this.flixService.symbols(URI.create(params.getTextDocument().getUri()));
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
		this.flixService.compile();
	}
}