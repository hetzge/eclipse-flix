package de.hetzge.eclipse.flix.server;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
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
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

public final class FlixTextDocumentService implements TextDocumentService {

	private final FlixServerService flixService;

	public FlixTextDocumentService(FlixServerService flixService) {
		this.flixService = flixService;
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return this.flixService.complete(position).thenApply(completionList -> {
			return Either.forRight(completionList);
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declaration(DeclarationParams params) {
		return this.flixService.decleration(params).thenApply(completionList -> {
			return Either.forRight(completionList);
		});
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return this.flixService.resolveCodeLens(params);
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return this.flixService.references(params);
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return this.flixService.hover(params);
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
		return this.flixService.documentSymbols(URI.create(params.getTextDocument().getUri()));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return this.flixService.rename(params);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didSave()");
		System.out.println("--------------\n" + params.getText() + "\n--------------");
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didOpen()");
		System.out.println("--------------\n" + params.getTextDocument().getText() + "\n--------------");
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didClose()");
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		System.out.println("FlixTextDocumentService.didChange()");
		for (final TextDocumentContentChangeEvent contentChange : params.getContentChanges()) {
			System.out.println("--------------\n" + contentChange.getText() + "\n--------------");
		}

		final List<TextDocumentContentChangeEvent> contentChangesEvents = params.getContentChanges();
		for (final TextDocumentContentChangeEvent contentChangeEvent : contentChangesEvents) {
			this.flixService.addUri(URI.create(params.getTextDocument().getUri()), contentChangeEvent.getText());
		}
		this.flixService.compile();
	}
}