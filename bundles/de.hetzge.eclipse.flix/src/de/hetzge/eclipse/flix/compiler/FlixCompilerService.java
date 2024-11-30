package de.hetzge.eclipse.flix.compiler;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.Throttler;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.swt.widgets.Display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.utils.GsonUtils;
import de.hetzge.eclipse.utils.Utils;

// https://andzac.github.io/anwn/Development%20docs/Language%20Server/ClientServerHandshake/

public final class FlixCompilerService {
	private static final ILog LOG = Platform.getLog(FlixCompilerService.class);

	private final FlixProject flixProject;
	private final FlixCompilerClient compilerClient;
	private final Set<String> diagnosticUris;
	private LanguageClient client;
	private final Throttler compileThrottler;

	public FlixCompilerService(FlixProject flixProject, FlixCompilerClient compilerClient) {
		this.flixProject = flixProject;
		this.compilerClient = compilerClient;
		this.diagnosticUris = new HashSet<>();
		this.compileThrottler = new Throttler(Display.getDefault(), Duration.ofSeconds(1), this::compile);
	}

	public void addWorkspaceUris() {
		for (final IFile sourceFile : this.flixProject.getFlixSourceFiles()) {
			addFile(sourceFile);
		}
		for (final IFile libraryFile : this.flixProject.getFlixFpkgLibraryFiles()) {
			addFile(libraryFile);
		}
		for (final IFile libraryFile : this.flixProject.getFlixJarLibraryFiles()) {
			addFile(libraryFile);
		}
	}

	public boolean addFile(IFile file) {
		final URI uri = file.getLocationURI();
		if (this.flixProject.isFlixSourceFile(file)) {
			addUri(uri, Utils.readFileContent(file));
			return true;
		} else if (this.flixProject.isFlixFpkgLibraryFile(file)) {
			addFpkg(file.getLocationURI());
			return true;
		} else if (this.flixProject.isFlixJarLibraryFile(file)) {
			addJar(file.getLocationURI());
			return true;
		} else {
			// ignore
			return false;
		}
	}

	public boolean removeFile(IFile file) {
		final URI uri = file.getLocationURI();
		if (this.flixProject.isFlixSourceFile(file)) {
			removeUri(uri);
			return true;
		} else if (this.flixProject.isFlixFpkgLibraryFile(file)) {
			removeFpkg(uri);
			return true;
		} else if (this.flixProject.isFlixJarLibraryFile(file)) {
			removeJar(uri);
			return true;
		} else {
			// ignore
			return false;
		}
	}

	public void addUri(URI uri, String content) {
		this.compilerClient.sendAddUri(uri, content);
	}

	public void removeUri(URI uri) {
		this.compilerClient.sendRemoveUri(uri);
	}

	public void addFpkg(URI uri) {
		this.compilerClient.sendFpkg(uri, Utils.readUriBase64Encoded(uri));
	}

	public void removeFpkg(URI uri) {
		this.compilerClient.sendRemoveFpkg(uri);
	}

	public void addJar(URI uri) {
		this.compilerClient.sendJar(uri);
	}

	public void removeJar(URI uri) {
		this.compilerClient.sendRemoveJar(uri);
	}

	public CompletableFuture<CompletionList> complete(CompletionParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendComplete(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return CompletableFuture.completedFuture(GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), CompletionList.class));
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<List<LocationLink>> decleration(DeclarationParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendGoto(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final JsonElement jsonElement = response.getSuccessJsonElement().get();
				final LocationLink link = GsonUtils.getGson().fromJson(jsonElement, LocationLink.class);
				link.setTargetUri(fixLibraryUri(link.getTargetUri()));
				return CompletableFuture.completedFuture(List.of(link));
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<Hover> hover(HoverParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendHover(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return CompletableFuture.completedFuture(GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), Hover.class));
			} else {
				return handleError(response);
			}
		});
	}

	public synchronized void syncCompile() {
		try {
			compile().get();
		} catch (InterruptedException | ExecutionException exception) {
			throw new RuntimeException(exception);
		}
	}

	public void asyncCompile() {
		this.compileThrottler.throttledExec();
	}

	private CompletableFuture<Void> compile() {
		System.out.println("FlixCompilerService.compile()");
		return this.compilerClient.sendCheck().thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				synchronized (this.diagnosticUris) {
					final JsonArray jsonArray = response.getSuccessJsonElement().orElse(new JsonArray()).getAsJsonArray();
					final Map<String, List<Diagnostic>> diagnosticsByUri = jsonArray.asList().stream()
							.map(jsonElement -> GsonUtils.getGson().fromJson(jsonElement, PublishDiagnosticsParams.class))
							.collect(Collectors.groupingBy(PublishDiagnosticsParams::getUri, Collectors.flatMapping(it -> it.getDiagnostics().stream(), Collectors.toList())));
					final Set<String> diff = new HashSet<>(this.diagnosticUris);
					// Set new diagnostics
					for (final Entry<String, List<Diagnostic>> entry : diagnosticsByUri.entrySet()) {
						this.client.publishDiagnostics(new PublishDiagnosticsParams(entry.getKey(), entry.getValue()));
						this.diagnosticUris.add(entry.getKey());
						diff.remove(entry.getKey());
					}
					// Unset all old diagnostics
					for (final String uri : diff) {
						this.client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
					}
				}
				return CompletableFuture.completedFuture(null);
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbols(URI uri) {
		return this.compilerClient.sendDocumentSymbols(unfixLibraryUri(uri.toString())).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>();
				final JsonArray jsonArray = response.getSuccessJsonElement().get().getAsJsonArray();
				for (final JsonElement jsonElement : jsonArray) {
					result.add(Either.forRight(GsonUtils.getGson().fromJson(jsonElement, DocumentSymbol.class)));
				}
				return CompletableFuture.completedFuture(result);
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendDocumentHighlight(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return CompletableFuture.completedFuture(GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), new TypeToken<List<DocumentHighlight>>() {
				}.getType()));
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> workspaceSymbols(WorkspaceSymbolParams params) {
		return this.compilerClient.sendWorkspaceSymbols(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final List<WorkspaceSymbol> result = new ArrayList<>();
				final JsonArray jsonArray = response.getSuccessJsonElement().get().getAsJsonArray();
				loop: for (final JsonElement jsonElement : jsonArray) {
					final WorkspaceSymbol workspaceSymbol = GsonUtils.getGson().fromJson(jsonElement, WorkspaceSymbol.class);
					final Either<Location, WorkspaceSymbolLocation> location = workspaceSymbol.getLocation();
					if (location.isLeft()) {
						location.getLeft().setUri(fixLibraryUri(location.getLeft().getUri()));
						if ("<unknown>".equals(location.getLeft().getUri())) { //$NON-NLS-1$
							LOG.info("Skip symbol because uri is '<unknown>'"); //$NON-NLS-1$
							continue loop;
						}
					} else {
						location.getRight().setUri(fixLibraryUri(location.getRight().getUri()));
						if ("<unknown>".equals(location.getRight().getUri())) { //$NON-NLS-1$
							LOG.info("Skip symbol because uri is '<unknown>'"); //$NON-NLS-1$
							continue loop;
						}
					}
					result.add(workspaceSymbol);
				}
				return CompletableFuture.completedFuture(Either.forRight(result));
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendRename(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return CompletableFuture.completedFuture(GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), WorkspaceEdit.class));
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendUses(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final List<Location> locations = GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), new TypeToken<List<Location>>() {
				}.getType());
				return CompletableFuture.completedFuture(locations.stream().filter(location -> !"<unknown>".equals(location.getUri())).collect(Collectors.toList())); //$NON-NLS-1$
			} else {
				return handleError(response);
			}
		});
	}

	public CompletableFuture<List<? extends CodeLens>> resolveCodeLens(CodeLensParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendCodeLens(params).thenCompose(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final List<? extends CodeLens> codeLenses;
				if (response.getSuccessJsonElement().get().isJsonArray()) {
					codeLenses = GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), new TypeToken<List<CodeLens>>() {
					}.getType());
				} else {
					codeLenses = List.of();
				}
				return CompletableFuture.completedFuture(codeLenses.stream().filter(codeLens -> Set.of("flix.runMain", "flix.cmdRepl").contains(codeLens.getCommand().getCommand())).collect(Collectors.toList())); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				return handleError(response);
			}
		});
	}

	public void setClient(LanguageClient client) {
		this.client = client;
	}

	private String fixLibraryUri(String uri) {
		if (uri.startsWith("file:")) { //$NON-NLS-1$
			return uri;
		}
		return this.flixProject.getProject().getFolder("library").getLocationURI() + "/" + uri; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String unfixLibraryUri(String uri) {
		final String libraryPrefix = this.flixProject.getProject().getFolder("library").getLocationURI().toString(); //$NON-NLS-1$
		if (!uri.startsWith(libraryPrefix)) {
			return uri;
		}
		return uri.replace(libraryPrefix, "");
	}

	private <T> CompletableFuture<T> handleError(FlixCompilerResponse response) {
		if (response.isInvalidRequest()) {
			LOG.warn("Invalid request: " + response.getFailureJsonElement().toString());
			return new CompletableFuture<T>(); // never complete
		} else {
			throw new RuntimeException(response.getFailureJsonElement().toString());
		}
	}

}
