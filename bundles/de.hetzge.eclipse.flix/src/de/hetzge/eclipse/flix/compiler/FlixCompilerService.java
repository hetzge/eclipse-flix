package de.hetzge.eclipse.flix.compiler;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import de.hetzge.eclipse.flix.FlixProjectBuilder;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.utils.FlixUtils;
import de.hetzge.eclipse.flix.utils.GsonUtils;
import de.hetzge.eclipse.utils.Utils;

// https://andzac.github.io/anwn/Development%20docs/Language%20Server/ClientServerHandshake/

public final class FlixCompilerService {
	private static final ILog LOG = Platform.getLog(FlixProjectBuilder.class);

	private final FlixProject flixProject;
	private final FlixCompilerClient compilerClient;
	private final Map<String, PublishDiagnosticsParams> diagnosticsParamsByUri;
	private LanguageClient client;

	public FlixCompilerService(FlixProject flixProject, FlixCompilerClient compilerClient) {
		this.flixProject = flixProject;
		this.compilerClient = compilerClient;
		this.diagnosticsParamsByUri = new HashMap<>();
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

	public void addFile(IFile file) {
		final URI uri = file.getLocationURI();
		if (this.flixProject.isFlixSourceFile(file)) {
			addUri(uri, Utils.readFileContent(file));
		} else if (this.flixProject.isFlixFpkgLibraryFile(file)) {
			addFpkg(file.getLocationURI());
		} else if (this.flixProject.isFlixJarLibraryFile(file)) {
			addJar(file.getLocationURI());
		} else {
			// ignore
		}
	}

	public void removeFile(IFile file) {
		final URI uri = file.getLocationURI();
		if (this.flixProject.isFlixSourceFile(file)) {
			removeUri(uri);
		} else if (this.flixProject.isFlixFpkgLibraryFile(file)) {
			removeFpkg(uri);
		} else if (this.flixProject.isFlixJarLibraryFile(file)) {
			removeJar(uri);
		} else {
			// ignore
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
		return this.compilerClient.sendComplete(params).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), CompletionList.class);
			} else {
				throw new RuntimeException();
			}
		});
	}

	public CompletableFuture<List<LocationLink>> decleration(DeclarationParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendGoto(params).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final LocationLink link = GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), LocationLink.class);
				link.setTargetUri(fixLibraryUri(link.getTargetUri()));
				return List.of(link);
			} else {
				throw new RuntimeException();
			}
		});
	}

	public CompletableFuture<Hover> hover(HoverParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendHover(params).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), Hover.class);
			} else {
				throw new RuntimeException(response.getFailureJsonElement().toString());
			}
		});
	}

	public CompletableFuture<Void> compile() {
		return this.compilerClient.sendCheck().thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				synchronized (this.diagnosticsParamsByUri) {
					final JsonArray jsonArray = response.getSuccessJsonElement().orElse(new JsonArray()).getAsJsonArray();
					final Map<String, PublishDiagnosticsParams> diffMap = new HashMap<>(this.diagnosticsParamsByUri);
					// Set all new diagnostics
					for (final JsonElement jsonElement : jsonArray) {
						final PublishDiagnosticsParams publishDiagnosticsParams = GsonUtils.getGson().fromJson(jsonElement, PublishDiagnosticsParams.class);
						this.diagnosticsParamsByUri.put(publishDiagnosticsParams.getUri(), publishDiagnosticsParams);
						diffMap.remove(publishDiagnosticsParams.getUri());
						this.client.publishDiagnostics(publishDiagnosticsParams);
					}
					// Unset all other diagnostics
					for (final PublishDiagnosticsParams diagnosticsParams : diffMap.values()) {
						this.client.publishDiagnostics(new PublishDiagnosticsParams(diagnosticsParams.getUri(), List.of()));
					}
				}
			}
			return null;
		});
	}

	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbols(URI uri) {
		return this.compilerClient.sendDocumentSymbols(unfixLibraryUri(uri.toString())).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>();
				final JsonArray jsonArray = response.getSuccessJsonElement().get().getAsJsonArray();
				for (final JsonElement jsonElement : jsonArray) {
					result.add(Either.forRight(GsonUtils.getGson().fromJson(jsonElement, DocumentSymbol.class)));
				}
				return result;
			} else {
				throw new RuntimeException(response.getFailureJsonElement().toString());
			}
		});
	}

	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendDocumentHighlight(params).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), new TypeToken<List<DocumentHighlight>>() {
				}.getType());
			} else {
				throw new RuntimeException(response.getFailureJsonElement().toString());
			}
		});
	}

	public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> workspaceSymbols(WorkspaceSymbolParams params) {
		return this.compilerClient.sendWorkspaceSymbols(params).thenApply(response -> {
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
				return Either.forRight(result);
			} else {
				throw new RuntimeException(response.getFailureJsonElement().toString());
			}
		});
	}

	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendRename(params).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				return GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), WorkspaceEdit.class);
			} else {
				throw new RuntimeException(response.getFailureJsonElement().toString());
			}
		});
	}

	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendUses(params).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final List<Location> locations = GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), new TypeToken<List<Location>>() {
				}.getType());
				return locations.stream().filter(location -> !"<unknown>".equals(location.getUri())).collect(Collectors.toList()); //$NON-NLS-1$
			} else {
				throw new RuntimeException(response.getFailureJsonElement().toString());
			}
		});
	}

	public CompletableFuture<List<? extends CodeLens>> resolveCodeLens(CodeLensParams params) {
		params.getTextDocument().setUri(unfixLibraryUri(params.getTextDocument().getUri().toString()));
		return this.compilerClient.sendCodeLens(params).thenApply(response -> {
			if (response.getSuccessJsonElement().isPresent()) {
				final List<? extends CodeLens> codeLenses;
				if (response.getSuccessJsonElement().get().isJsonArray()) {
					codeLenses = GsonUtils.getGson().fromJson(response.getSuccessJsonElement().get(), new TypeToken<List<CodeLens>>() {
					}.getType());
				} else {
					codeLenses = List.of();
				}
				return codeLenses.stream().filter(codeLens -> Set.of("flix.runMain", "flix.cmdRepl").contains(codeLens.getCommand().getCommand())).collect(Collectors.toList()); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				throw new RuntimeException(response.getFailureJsonElement().toString());
			}
		});
	}

	public void setClient(LanguageClient client) {
		this.client = client;
	}

	private String fixLibraryUri(String uri) {
		if (uri.startsWith("file:")) {
			return uri;
		}
		return FlixUtils.loadFlixJarUri(this.flixProject.getFlixVersion(), null).toString() + "!/src/library/" + uri; //$NON-NLS-1$
	}

	private String unfixLibraryUri(String uri) {
		final String libraryPrefix = FlixUtils.loadFlixJarUri(this.flixProject.getFlixVersion(), null).toString() + "!/src/library/"; // $NON-NLS-1$
		if (!uri.startsWith(libraryPrefix)) {
			return uri;
		}
		return uri.replace(libraryPrefix, "");
	}

}
