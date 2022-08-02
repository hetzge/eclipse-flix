package de.hetzge.eclipse.flix.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;

public final class FlixLanguageServerSocketThread extends Thread implements AutoCloseable {
	private final FlixLanguageServer server;
	private final ExecutorService executorService;
	private boolean done;

	public FlixLanguageServerSocketThread(FlixLanguageServer server) {
		super("LSP Server Socket");
		this.server = server;
		this.executorService = Executors.newSingleThreadExecutor();
		this.done = false;
		setDaemon(true);
	}

	@Override
	public void run() {
		while (!this.done) {
			try (ServerSocket serverSocket = new ServerSocket(10587)) {
				final Socket socket = serverSocket.accept();
				new Builder<LanguageClient>() //
						.setLocalService(this.server) //
						.setRemoteInterface(LanguageClient.class) //
						.setInput(socket.getInputStream()) //
						.setOutput(socket.getOutputStream()) //
						.setExecutorService(this.executorService) //
						.traceMessages(new PrintWriter(System.out)) //
						.create() //
						.startListening();
			} catch (final IOException exception) {
				Activator.logError(exception);
			}
		}
	}

	@Override
	public void close() {
		this.done = true;
	}
}