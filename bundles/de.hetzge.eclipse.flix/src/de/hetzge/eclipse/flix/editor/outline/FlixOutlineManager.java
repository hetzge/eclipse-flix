package de.hetzge.eclipse.flix.editor.outline;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentSymbolProvider;
import org.lxtk.LanguageService;
import org.lxtk.util.Disposable;

public final class FlixOutlineManager {

	private final LanguageService languageService;

	public FlixOutlineManager(LanguageService languageService) {
		this.languageService = languageService;
	}

	public CompletableFuture<Outline> get(URI uri) {
		final CompletableFuture<DocumentSymbolProvider> future = new CompletableFuture<>();
		for (final DocumentSymbolProvider provider : this.languageService.getDocumentSymbolProviders()) {
			future.complete(provider);
			break;
		}
		if (!future.isDone()) {
			final Disposable[] disposable = new Disposable[1];
			disposable[0] = this.languageService.getDocumentSymbolProviders().onDidAdd().subscribe(provider -> {
				try {
					future.complete(provider);
				} finally {
					if (disposable[0] != null) {
						disposable[0].dispose();
					}
				}
			});
		}
		return future.thenCompose(provider -> {
			return provider.getDocumentSymbols(new DocumentSymbolParams(new TextDocumentIdentifier(uri.toString())));
		}).thenApply(result -> {
			return new Outline(result.stream().map(Either::getRight).collect(Collectors.toList()));
		});
	}

	public static class Outline {
		private final List<DocumentSymbol> rootSymbols;

		public Outline(List<DocumentSymbol> rootSymbols) {
			this.rootSymbols = rootSymbols;
		}

		public List<DocumentSymbol> getRootSymbols() {
			return this.rootSymbols;
		}
	}
}
