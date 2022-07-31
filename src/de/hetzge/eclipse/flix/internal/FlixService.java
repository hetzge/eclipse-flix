package de.hetzge.eclipse.flix.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.lxtk.util.connect.StreamBasedConnection;

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
			this.compilerClient.sendAddUri(file.getLocationURI(), new String(file.getContents().readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException | CoreException exception) {
			throw new RuntimeException(exception);
		}
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

	public StreamBasedConnection getConnection() {
		try {
			System.out.println("FlixService.getConnection()");



			final PipedOutputStream clientToServerStream = new PipedOutputStream();
			final PipedInputStream serverToClientStream = new PipedInputStream();
			final InputStream clientToServerStreamReverse = new BufferedInputStream(new PipedInputStream(clientToServerStream));
			final OutputStream serverToClientStreamReverse = new BufferedOutputStream(new PipedOutputStream(serverToClientStream));
			new Builder<LanguageClient>() //
					.setLocalService(this.server) //
					.setRemoteInterface(LanguageClient.class) //
					.setInput(clientToServerStreamReverse) //
					.setOutput(serverToClientStreamReverse) //
					.setExecutorService(Executors.newSingleThreadExecutor()) //
					.traceMessages(new PrintWriter(System.out)) //
					.create() //
					.startListening();

			final CompletableFuture<Object> disposeFuture = new CompletableFuture<>();
			return new StreamBasedConnection() {

				@Override
				public void dispose() {
					System.out.println("FlixService.getConnection().new StreamBasedConnection() {...}.dispose()");
					disposeFuture.complete(null);
				}

				@Override
				public CompletionStage<?> onDispose() {
					return disposeFuture;
				}

				@Override
				public boolean isClosed() {
					return onDispose().toCompletableFuture().isDone();
				}

				@Override
				public OutputStream getOutputStream() {
					return clientToServerStream;
				}

				@Override
				public InputStream getInputStream() {
					return serverToClientStream;
				}
			};
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}
}
