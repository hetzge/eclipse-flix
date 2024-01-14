package de.hetzge.eclipse.flix.editor.outline;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
	private final Map<URI, Outline> cache;

	public FlixOutlineManager(LanguageService languageService) {
		this.languageService = languageService;
		this.cache = new ConcurrentHashMap<>();
	}

	public CompletableFuture<Outline> queryOutline(URI uri) {
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
			final Outline outline = new Outline(result.stream().map(Either::getRight).collect(Collectors.toList()));
			this.cache.put(uri, outline);
			return outline;
		});
	}

	public CompletableFuture<Outline> queryOutlinePreferCache(URI uri) {
		final Outline cachedOutline = this.cache.get(uri);
		return cachedOutline != null ? CompletableFuture.completedFuture(cachedOutline) : queryOutline(uri);
	}

	public static class Outline {
		private final List<DocumentSymbol> rootSymbols;

		public Outline(List<DocumentSymbol> rootSymbols) {
			this.rootSymbols = rootSymbols;
		}

		public List<DocumentSymbol> getRootSymbols() {
			return this.rootSymbols;
		}

		public void visitPaths(Consumer<LinkedList<DocumentSymbol>> consumer) {
			for (final DocumentSymbol documentSymbol : this.rootSymbols) {
				consumer.accept(new LinkedList<>(List.of(documentSymbol)));
				visitPaths(List.of(documentSymbol), consumer);
			}
		}

		private void visitPaths(List<DocumentSymbol> parents, Consumer<LinkedList<DocumentSymbol>> consumer) {
			if (parents.isEmpty()) {
				return;
			}
			for (final DocumentSymbol child : parents.get(parents.size() - 1).getChildren()) {
				final LinkedList<DocumentSymbol> newParents = new LinkedList<>();
				newParents.addAll(parents);
				newParents.add(child);
				visitPaths(newParents, consumer);
			}
		}
	}
}
