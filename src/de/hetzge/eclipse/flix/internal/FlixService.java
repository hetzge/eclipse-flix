package de.hetzge.eclipse.flix.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;

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

		final ExecutorService executorService = Executors.newSingleThreadExecutor();
		new Thread("LSP Server Socket") {
			@Override
			public void run() {
				while (true) {
					try (ServerSocket serverSocket = new ServerSocket(10587)) {
						final Socket socket = serverSocket.accept();
						new Builder<LanguageClient>() //
								.setLocalService(FlixService.this.server) //
								.setRemoteInterface(LanguageClient.class) //
								.setInput(socket.getInputStream()) //
								.setOutput(socket.getOutputStream()) //
								.setExecutorService(executorService) //
								.traceMessages(new PrintWriter(System.out)) //
								.create() //
								.startListening();
					} catch (final IOException exception) {
						Activator.logError(exception);
					}
				}
			};
		}.start();
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
}
