package de.hetzge.eclipse.flix.server;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.LocationLink;

import de.hetzge.eclipse.flix.GsonUtils;
import de.hetzge.eclipse.flix.compiler.FlixCompilerClient;
import de.hetzge.eclipse.flix.compiler.FlixCompilerProcess;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

// https://andzac.github.io/anwn/Development%20docs/Language%20Server/ClientServerHandshake/

public final class FlixServerService implements AutoCloseable {

	private static final String FLIX_FILE_EXTENSION = "flix";
	private static final String FLIX_PACKAGE_FILE_EXTENSION = "fpkg";
	private static final String JAR_FILE_EXTENSION = "jar";

	private final IProject project;
	private final FlixCompilerClient compilerClient;
	private final FlixCompilerProcess compilerProcess;

	public FlixServerService(IProject project, FlixCompilerClient compilerClient, FlixCompilerProcess compilerProcess) {
		this.project = project;
		this.compilerClient = compilerClient;
		this.compilerProcess = compilerProcess;
	}

	@Override
	public void close() {
		System.out.println("FlixServerService.close()");
		this.compilerClient.close();
		this.compilerProcess.close();
	}

	public void addWorkspaceUris() {
		EclipseUtils.visitFiles(this.project, this::addFile);
	}

	public void addFile(IFile file) {
		final String fileExtension = file.getFileExtension();
		final URI uri = file.getLocationURI();
		if (FLIX_FILE_EXTENSION.equals(fileExtension)) {
			addUri(uri, Utils.readFileContent(file));
		} else if (isInLibFolder(file) && FLIX_PACKAGE_FILE_EXTENSION.equals(fileExtension)) {
			addFpkg(file.getLocationURI());
		} else if (isInLibFolder(file) && JAR_FILE_EXTENSION.equals(fileExtension)) {
			addJar(file.getLocationURI());
		} else {
			System.out.println("Ignore '" + uri + "'");
		}
	}

	public void removeFile(IFile file) {
		final String fileExtension = file.getFileExtension();
		final URI uri = file.getLocationURI();
		if (FLIX_FILE_EXTENSION.equals(fileExtension)) {
			removeUri(uri);
		} else if (isInLibFolder(file) && FLIX_PACKAGE_FILE_EXTENSION.equals(fileExtension)) {
			removeFpkg(uri);
		} else if (isInLibFolder(file) && JAR_FILE_EXTENSION.equals(fileExtension)) {
			removeJar(uri);
		} else {
			System.out.println("Ignore '" + uri + "'");
		}
	}

	private boolean isInLibFolder(IFile file) {
		return this.project.getFullPath().append("/lib/").isPrefixOf(file.getFullPath());
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
		this.compilerClient.sendJar(uri, Utils.readUriBase64Encoded(uri));
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
				return List.of(GsonUtils.getGson().fromJson(response.getLeft(), LocationLink.class));
			} else {
				throw new RuntimeException();
			}
		});
	}

	public CompletableFuture<Hover> hover(HoverParams params) {
		return this.compilerClient.sendHover(params).thenApply(response -> {
			if (response.isLeft()) {
				return GsonUtils.getGson().fromJson(response.getLeft(), Hover.class);
			} else {
				throw new RuntimeException();
			}
		});
	}

	public CompletableFuture<Void> compile() {
		return this.compilerClient.sendCheck().thenApply(ignore -> null);
	}
}
