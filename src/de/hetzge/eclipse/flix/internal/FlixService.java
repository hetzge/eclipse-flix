package de.hetzge.eclipse.flix.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.Location;

import com.google.gson.reflect.TypeToken;

// https://andzac.github.io/anwn/Development%20docs/Language%20Server/ClientServerHandshake/

public final class FlixService {

	private static final String FLIX_FILE_EXTENSION = "flix";

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
		visitFiles(ResourcesPlugin.getWorkspace().getRoot(), file -> {
			if (FLIX_FILE_EXTENSION.equals(file.getFileExtension())) {
				addUri(file);
			}
		});
	}

	public void addUri(IFile file) {
		try {
			this.addUri(file.getLocationURI(), new String(file.getContents().readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException | CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	public void addUri(URI uri, String content) {
		this.compilerClient.sendAddUri(uri, content);
	}

	public void removeUri(IFile file) {
		this.compilerClient.sendRemoveUri(file.getLocationURI());
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

	private void visitFiles(IContainer container, Consumer<IFile> fileConsumer) {
		try {
			for (final IResource member : container.members()) {
				if (member instanceof IContainer) {
					visitFiles((IContainer) member, fileConsumer);
				} else if (member instanceof IFile) {
					fileConsumer.accept((IFile) member);
				}
			}
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}
}
