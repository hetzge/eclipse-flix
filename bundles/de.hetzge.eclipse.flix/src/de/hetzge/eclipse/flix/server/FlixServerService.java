package de.hetzge.eclipse.flix.server;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
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
import org.lxtk.util.SafeRun.Rollback;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import de.hetzge.eclipse.flix.compiler.FlixCompilerClient;
import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.flix.utils.FlixUtils;
import de.hetzge.eclipse.flix.utils.GsonUtils;
import de.hetzge.eclipse.utils.Utils;

// https://andzac.github.io/anwn/Development%20docs/Language%20Server/ClientServerHandshake/

public final class FlixServerService implements AutoCloseable {

	private final IFlixProject flixProject;
	private final FlixCompilerClient compilerClient;
	private final Map<String, PublishDiagnosticsParams> diagnosticsParamsByUri;
	private final Rollback rollback;
	private LanguageClient client;

	FlixServerService(IFlixProject flixProject, FlixCompilerClient compilerClient, Rollback rollback) {
		this.flixProject = flixProject;
		this.compilerClient = compilerClient;
		this.rollback = rollback;
		this.diagnosticsParamsByUri = new HashMap<>();
	}

	@Override
	public void close() {
		System.out.println("FlixServerService.close()");
		this.rollback.run();
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
			System.out.println("[" + this.flixProject.getProject().getName() + "] Ignore '" + uri + "'");
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
			System.out.println("[" + this.flixProject.getProject().getName() + "] Ignore '" + uri + "'");
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
		return this.compilerClient.sendComplete(params).thenApply(response -> {
			if (response.isLeft()) {
				return GsonUtils.getGson().fromJson(response.getLeft(), CompletionList.class);
			} else {
				throw new RuntimeException();
			}
		});
	}

	public CompletableFuture<List<LocationLink>> decleration(DeclarationParams params) {
		return this.compilerClient.sendGoto(params).thenApply(response -> {
			if (response.isLeft()) {
				final LocationLink link = GsonUtils.getGson().fromJson(response.getLeft(), LocationLink.class);
				link.setTargetUri(fixLibraryUri(link.getTargetUri()));
				return List.of(link);
			} else {
				throw new RuntimeException();
			}
		});
	}

	private String fixLibraryUri(String targetUriValue) {
		if (targetUriValue.endsWith(".flix") && !targetUriValue.startsWith("file:")) {
			final Path libraryFolderPath = FlixUtils.loadFlixLibraryFolderPath(this.flixProject.getFlixVersion(), null);
			final Path sourcePath = libraryFolderPath.resolve(targetUriValue);
			return sourcePath.toUri().toASCIIString();
		} else {
			return targetUriValue;
		}
	}

	public CompletableFuture<Hover> hover(HoverParams params) {
		return this.compilerClient.sendHover(params).thenApply(response -> {
			if (response.isLeft()) {
				return GsonUtils.getGson().fromJson(response.getLeft(), Hover.class);
			} else {
				throw new RuntimeException(response.getRight().toString());
			}
		});
	}

	public CompletableFuture<Void> compile() {
		return this.compilerClient.sendCheck().thenApply(response -> {
			synchronized (this.diagnosticsParamsByUri) {
				if (response.isLeft()) {
					for (final PublishDiagnosticsParams diagnosticsParams : this.diagnosticsParamsByUri.values()) {
						this.client.publishDiagnostics(new PublishDiagnosticsParams(diagnosticsParams.getUri(), List.of()));
					}
					return null;
				} else {
					final JsonArray jsonArray = response.getRight().getAsJsonArray();
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
					return null;
				}
			}
		});
	}

	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbols(URI uri) {
		return this.compilerClient.sendDocumentSymbols(uri).thenApply(response -> {
			if (response.isLeft()) {
				final List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>();
				final JsonArray jsonArray = response.getLeft().getAsJsonArray();
				for (final JsonElement jsonElement : jsonArray) {
					result.add(Either.forRight(GsonUtils.getGson().fromJson(jsonElement, DocumentSymbol.class)));
				}
				return result;
			} else {
				throw new RuntimeException(response.getRight().toString());
			}
		});
	}

	public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> workspaceSymbols(WorkspaceSymbolParams params) {
		return this.compilerClient.sendWorkspaceSymbols(params).thenApply(response -> {
			if (response.isLeft()) {
				final List<WorkspaceSymbol> result = new ArrayList<>();
				final JsonArray jsonArray = response.getLeft().getAsJsonArray();
				loop: for (final JsonElement jsonElement : jsonArray) {
					final WorkspaceSymbol workspaceSymbol = GsonUtils.getGson().fromJson(jsonElement, WorkspaceSymbol.class);
					final Either<Location, WorkspaceSymbolLocation> location = workspaceSymbol.getLocation();
					if (location.isLeft()) {
						location.getLeft().setUri(fixLibraryUri(location.getLeft().getUri()));
						if ("<unknown>".equals(location.getLeft().getUri())) {
							System.out.println("Skip symbol because uri is '<unknown>'");
							continue loop;
						}
					} else {
						location.getRight().setUri(fixLibraryUri(location.getRight().getUri()));
						if ("<unknown>".equals(location.getRight().getUri())) {
							System.out.println("Skip symbol because uri is '<unknown>'");
							continue loop;
						}
					}
					result.add(workspaceSymbol);
				}
				return Either.forRight(result);
			} else {
				throw new RuntimeException(response.getRight().toString());
			}
		});
	}

	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return this.compilerClient.sendRename(params).thenApply(response -> {
			if (response.isLeft()) {
				return GsonUtils.getGson().fromJson(response.getLeft(), WorkspaceEdit.class);
			} else {
				throw new RuntimeException(response.getRight().toString());
			}
		});
	}

	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return this.compilerClient.sendUses(params).thenApply(response -> {
			if (response.isLeft()) {
				return GsonUtils.getGson().fromJson(response.getLeft(), new TypeToken<List<Location>>() {
				}.getType());
			} else {
				throw new RuntimeException(response.getRight().toString());
			}
		});
	}

	public CompletableFuture<List<? extends CodeLens>> resolveCodeLens(CodeLensParams params) {
		return this.compilerClient.sendCodeLens(params).thenApply(response -> {
			if (response.isLeft()) {
				// TODO No longer list
				final List<? extends CodeLens> codeLenses = GsonUtils.getGson().fromJson(response.getLeft(), new TypeToken<List<CodeLens>>() {
				}.getType());
				return codeLenses.stream().filter(codeLens -> Set.of("flix.runMain", "flix.cmdRepl").contains(codeLens.getCommand().getCommand())).collect(Collectors.toList());
			} else {
				throw new RuntimeException(response.getRight().toString());
			}
		});
	}

	public void setClient(LanguageClient client) {
		this.client = client;
	}
}
