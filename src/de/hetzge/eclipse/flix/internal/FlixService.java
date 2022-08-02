package de.hetzge.eclipse.flix.internal;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.Location;

import com.google.gson.reflect.TypeToken;

import de.hetzge.eclipse.EclipseUtils;
import de.hetzge.eclipse.Utils;

// https://andzac.github.io/anwn/Development%20docs/Language%20Server/ClientServerHandshake/

public final class FlixService {

	private static final String FLIX_FILE_EXTENSION = "flix";
	private static final String FLIX_PACKAGE_FILE_EXTENSION = "fpkg";
	private static final String JAR_FILE_EXTENSION = "jar";

	private FlixCompilerClient compilerClient;
	private FlixCompilerProcess compilerProcess;
	private final FlixLanguageServer server;

	public FlixService() {
		this.server = new FlixLanguageServer(this);
	}

	public void initialize() {
		System.out.println("FlixService.initialize()");
		this.compilerProcess = FlixCompilerProcess.start();
		this.compilerClient = FlixCompilerClient.connect();
		new FlixLanguageServerSocketThread(this.server).start();
	}

	public void addWorkspaceUris() {
		EclipseUtils.visitFiles(ResourcesPlugin.getWorkspace().getRoot(), this::addFile);
	}

	public void addFile(IFile file) {
		try {
			final String fileExtension = file.getFileExtension();
			final URI uri = file.getLocationURI();
			if (FLIX_FILE_EXTENSION.equals(fileExtension)) {
				addUri(uri, Utils.readFileContent(file));
			} else if (FLIX_PACKAGE_FILE_EXTENSION.equals(fileExtension)) {
				addUri(uri, Utils.readFileContent(file));
			} else if (JAR_FILE_EXTENSION.equals(fileExtension)) {
				addUri(uri, Utils.readFileContent(file));
			} else {
				System.out.println("Ignore '" + uri + "'");
			}
		} catch (IOException | CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	public void removeFile(IFile file) {
		final String fileExtension = file.getFileExtension();
		final URI uri = file.getLocationURI();
		if (FLIX_FILE_EXTENSION.equals(fileExtension)) {
			removeUri(uri);
		} else if (FLIX_PACKAGE_FILE_EXTENSION.equals(fileExtension)) {
			removeFpkg(uri);
		} else if (JAR_FILE_EXTENSION.equals(fileExtension)) {
			removeJar(uri);
		} else {
			System.out.println("Ignore '" + uri + "'");
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
		this.compilerClient.sendJar(uri, Utils.readUriBase64Encoded(uri));
	}

	public void removeJar(URI uri) {
		this.compilerClient.sendRemoveJar(uri);
	}

	public CompletableFuture<CompletionList> complete(CompletionParams params) {
		return this.compilerClient.sendComplete(params).thenApply(response -> {
			return GsonUtils.getGson().fromJson(response, CompletionList.class);
		});
	}

	public CompletableFuture<List<Location>> decleration(DeclarationParams params) {
		return this.compilerClient.sendGoto(params).thenApply(response -> {
			return GsonUtils.getGson().fromJson(response, new TypeToken<List<Location>>() {
			}.getType());
		});
	}
}
