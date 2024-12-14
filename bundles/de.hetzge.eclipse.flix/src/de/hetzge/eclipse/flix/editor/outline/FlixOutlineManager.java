package de.hetzge.eclipse.flix.editor.outline;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentSymbolProvider;
import org.lxtk.LanguageService;
import org.lxtk.util.SafeRun;

public final class FlixOutlineManager {

	private final LanguageService languageService;
	private final Map<URI, Outline> cache;

	public FlixOutlineManager(LanguageService languageService) {
		this.languageService = languageService;
		this.cache = new ConcurrentHashMap<>();
	}

	public CompletableFuture<Outline> queryOutline(URI uri) {
		return SafeRun.runWithResult(rollback -> {
			final CompletableFuture<DocumentSymbolProvider> future = new CompletableFuture<>();
			future.thenRun(rollback::run);
			final Optional<DocumentSymbolProvider> providerOptional = getProvider(uri);
			if (providerOptional.isPresent()) {
				future.complete(providerOptional.get());
			} else {
				// Wait for the provider to be available
				rollback.add(this.languageService.getDocumentSymbolProviders().onDidAdd().subscribe(provider -> {
					getProvider(uri).ifPresent(future::complete);
				})::dispose);
			}
			return future.thenCompose(provider -> {
				return provider.getDocumentSymbols(new DocumentSymbolParams(new TextDocumentIdentifier(uri.toString())));
			}).thenApply(result -> {
				final Outline outline = new Outline(result.stream().map(Either::getRight).collect(Collectors.toList()));
				this.cache.put(uri, outline);
				return outline;
			}).orTimeout(10L, TimeUnit.SECONDS);
		});
	}

	public CompletableFuture<Outline> queryOutlinePreferCache(URI uri) {
		final Outline cachedOutline = this.cache.get(uri);
		return cachedOutline != null ? CompletableFuture.completedFuture(cachedOutline) : queryOutline(uri);
	}

	private Optional<DocumentSymbolProvider> getProvider(URI uri) {
		for (final DocumentSymbolProvider provider : this.languageService.getDocumentSymbolProviders()) {
			if (matchesProvider(provider, uri)) {
				return Optional.of(provider);
			}
		}
		return Optional.empty();
	}

	private static boolean matchesProvider(DocumentSymbolProvider provider, URI uri) {
		for (final DocumentFilter documentSelector : provider.getDocumentSelector()) {
			if (FileSystems.getDefault().getPathMatcher("glob:" + documentSelector.getPattern()).matches(Path.of(uri))) {
				return true;
			}
		}
		return false;
	}
}
