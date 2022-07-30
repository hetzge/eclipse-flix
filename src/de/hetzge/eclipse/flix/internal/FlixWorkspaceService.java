package de.hetzge.eclipse.flix.internal;

import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.lsp4j.CreateFilesParams;
import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.FileCreate;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

final class FlixWorkspaceService implements WorkspaceService {

	private final FlixService flixService;

	public FlixWorkspaceService(FlixService flixService) {
		this.flixService = flixService;
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		System.out.println("FlixWorkspaceService.didChangeWatchedFiles()");
		// ignore
	}

	@Override
	public void didDeleteFiles(DeleteFilesParams params) {
		System.out.println("FlixWorkspaceService.didDeleteFiles()");
		// ignore
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
				this.flixService.addUri(file);
			}
		}
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