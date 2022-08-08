package de.hetzge.eclipse.flix.server;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IProject;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.FileOperationFilter;
import org.eclipse.lsp4j.FileOperationOptions;
import org.eclipse.lsp4j.FileOperationPattern;
import org.eclipse.lsp4j.FileOperationsServerCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.lxtk.util.SafeRun;

import de.hetzge.eclipse.flix.compiler.FlixCompilerClient;
import de.hetzge.eclipse.flix.compiler.FlixCompilerProcess;
import de.hetzge.eclipse.utils.Utils;

// https://github.com/flix/flix/issues/806#issuecomment-612400296

public class FlixLanguageServer implements LanguageServer, AutoCloseable {

	private final FlixServerService flixService;

	public FlixLanguageServer(FlixServerService flixService) {
		this.flixService = flixService;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		return CompletableFuture.supplyAsync(() -> {
			System.out.println("FlixLanguageServer.initialize()");

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
			final SaveOptions saveOptions = new SaveOptions();
			saveOptions.setIncludeText(true);
			final TextDocumentSyncOptions textDocumentSyncOptions = new TextDocumentSyncOptions();
			textDocumentSyncOptions.setSave(true);
			textDocumentSyncOptions.setSave(saveOptions);
			final CompletionOptions completionProvider = new CompletionOptions();
			final ServerCapabilities capabilities = new ServerCapabilities();
			capabilities.setCodeLensProvider(null);
			capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
			capabilities.setWorkspace(workspaceServerCapabilities);
			capabilities.setDocumentFormattingProvider(true);
			capabilities.setCompletionProvider(completionProvider);
			capabilities.setHoverProvider(true);
			capabilities.setReferencesProvider(true);
			capabilities.setDeclarationProvider(true);
			final InitializeResult initializeResult = new InitializeResult();
			initializeResult.setCapabilities(capabilities);
			initializeResult.setServerInfo(new ServerInfo("Flix Eclipse Language Server", "0.1"));
			return initializeResult;
		});
	}

	@Override
	public void initialized(InitializedParams params) {
		System.out.println("FlixLanguageServer.initialized()");
		LanguageServer.super.initialized(params);

		this.flixService.addWorkspaceUris();
		this.flixService.compile();
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
		return new FlixTextDocumentService(this.flixService);
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return new FlixWorkspaceService(this.flixService);
	}

	@Override
	public void close() {
		System.out.println("FlixLanguageServer.close()");
		this.flixService.close();
	}

	public static FlixLanguageServer start(IProject project) {
		System.out.println("FlixLanguageServer.start()");
		return SafeRun.runWithResult(rollback -> {
			final int compilerPort = Utils.queryPort();
			final FlixCompilerProcess compilerProcess = FlixCompilerProcess.start(compilerPort);
			rollback.add(compilerProcess::close);
			final FlixCompilerClient compilerClient = FlixCompilerClient.connect(compilerPort);
			rollback.add(compilerClient::close);

			return new FlixLanguageServer(new FlixServerService(project, compilerClient, compilerProcess));
		});
	}

	public void setClient(LanguageClient client) {
		this.flixService.setClient(client);
	}
}
