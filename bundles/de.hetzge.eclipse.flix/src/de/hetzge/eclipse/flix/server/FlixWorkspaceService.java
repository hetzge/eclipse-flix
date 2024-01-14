package de.hetzge.eclipse.flix.server;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.lsp4j.CreateFilesParams;
import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.FileCreate;
import org.eclipse.lsp4j.FileDelete;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;

import de.hetzge.eclipse.flix.compiler.FlixCompilerService;

public final class FlixWorkspaceService implements WorkspaceService {

	private final FlixCompilerService flixService;

	public FlixWorkspaceService(FlixCompilerService flixService) {
		this.flixService = flixService;
	}

	@Override
	public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
		System.out.println("FlixWorkspaceService.symbol()");
		return this.flixService.workspaceSymbols(params);
	}

	@Override
	public CompletableFuture<WorkspaceEdit> willDeleteFiles(DeleteFilesParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<WorkspaceEdit> willCreateFiles(CreateFilesParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<WorkspaceEdit> willRenameFiles(RenameFilesParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		System.out.println("FlixWorkspaceService.didChangeWatchedFiles()");
		// ignore
	}

	@Override
	public void didDeleteFiles(DeleteFilesParams params) {
		System.out.println("FlixWorkspaceService.didDeleteFiles()");
		for (final FileDelete delete : params.getFiles()) {
			final IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(URI.create(delete.getUri()));
			for (final IFile file : files) {
				this.flixService.removeFile(file);
			}
		}
		this.flixService.compile();
	}

	@Override
	public void didRenameFiles(RenameFilesParams params) {
		System.out.println("FlixWorkspaceService.didRenameFiles()");
		// ignore
	}

	@Override
	public void didCreateFiles(CreateFilesParams params) {
		System.out.println("FlixWorkspaceService.didCreateFiles()");
		final List<FileCreate> fileCreates = params.getFiles();
		for (final FileCreate fileCreate : fileCreates) {
			final IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(URI.create(fileCreate.getUri()));
			for (final IFile file : files) {
				this.flixService.addFile(file);
			}
		}
		this.flixService.compile();
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		System.out.println("FlixWorkspaceService.didChangeConfiguration()");
		// ignore
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		System.out.println("FlixWorkspaceService.didChangeWorkspaceFolders()");
		// ignore
	}
}