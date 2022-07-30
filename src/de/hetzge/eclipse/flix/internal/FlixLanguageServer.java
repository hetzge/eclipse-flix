package de.hetzge.eclipse.flix.internal;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.FileOperationFilter;
import org.eclipse.lsp4j.FileOperationOptions;
import org.eclipse.lsp4j.FileOperationPattern;
import org.eclipse.lsp4j.FileOperationsServerCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

// https://github.com/flix/flix/issues/806#issuecomment-612400296

public class FlixLanguageServer implements LanguageServer {

	private final FlixService flixService;

	public FlixLanguageServer(FlixService flixService) {
		this.flixService = flixService;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		return CompletableFuture.supplyAsync(() -> {

			System.out.println("---->");
			System.out.println(params.getCapabilities().getWorkspace().getWorkspaceEdit().getResourceOperations().stream().collect(Collectors.joining(", ")));

			this.flixService.addWorkspaceUris();

			final FileOperationOptions fileOperationOptions = new FileOperationOptions();
			fileOperationOptions.setFilters(List.of(new FileOperationFilter(new FileOperationPattern("**"))));
			final FileOperationsServerCapabilities fileOperationsServerCapabilities = new FileOperationsServerCapabilities();
			fileOperationsServerCapabilities.setDidCreate(fileOperationOptions);
			fileOperationsServerCapabilities.setDidDelete(fileOperationOptions);
			fileOperationsServerCapabilities.setDidRename(fileOperationOptions);
			fileOperationsServerCapabilities.setWillCreate(fileOperationOptions);
			fileOperationsServerCapabilities.setWillDelete(fileOperationOptions);
			fileOperationsServerCapabilities.setWillRename(fileOperationOptions);
			final WorkspaceFoldersOptions workspaceFolders = new WorkspaceFoldersOptions();
			workspaceFolders.setSupported(true);
			final WorkspaceServerCapabilities workspaceServerCapabilities = new WorkspaceServerCapabilities();
			workspaceServerCapabilities.setFileOperations(fileOperationsServerCapabilities);
			workspaceServerCapabilities.setWorkspaceFolders(workspaceFolders);
			final CompletionOptions completionProvider = new CompletionOptions();
			final ServerCapabilities capabilities = new ServerCapabilities();
			capabilities.setCodeLensProvider(null);
			capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
			capabilities.setWorkspace(workspaceServerCapabilities);
			capabilities.setDocumentFormattingProvider(true);
			capabilities.setCompletionProvider(completionProvider);
			capabilities.setHoverProvider(true);
			final InitializeResult initializeResult = new InitializeResult();
			initializeResult.setCapabilities(capabilities);
			return initializeResult;
		});
	}

	@Override
	public void initialized(InitializedParams params) {
		System.out.println("FlixLanguageServer.initialized()");
		LanguageServer.super.initialized(params);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return CompletableFuture.supplyAsync(() -> {
			System.out.println("FlixLanguageServer.shutdown()");
			return "Hello";
		});
	}

	@Override
	public void exit() {
		System.out.println("FlixLanguageServer.exit()");
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return new FlixTextDocumentService();
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return new FlixWorkspaceService(this.flixService);
	}
}
