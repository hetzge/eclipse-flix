package de.hetzge.eclipse.flix.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import de.hetzge.eclipse.flix.FlixLogger;

public final class FlixLanguageServerSocketThread extends Thread implements AutoCloseable {
	private final int port;
	private final FlixLanguageServer server;
	private final ExecutorService executorService;
	private final List<Rollback> rollbacks;
	private boolean done;

	public FlixLanguageServerSocketThread(FlixLanguageServer server, int port) {
		super("LSP Server Socket");
		this.server = server;
		this.port = port;
		this.executorService = Executors.newSingleThreadExecutor();
		this.rollbacks = new ArrayList<>();
		this.done = false;
		setDaemon(true);
	}

	@Override
	public void run() {
		while (!this.done) {
			try (ServerSocket serverSocket = new ServerSocket(this.port)) {
				SafeRun.run(rollback -> {
					try {
						final Socket socket = serverSocket.accept();
						rollback.add(() -> {
							try {
								socket.close();
							} catch (final IOException exception) {
								throw new RuntimeException(exception);
							}
						});
						new Builder<LanguageClient>() //
								.setLocalService(this.server) //
								.setRemoteInterface(LanguageClient.class) //
								.setInput(socket.getInputStream()) //
								.setOutput(socket.getOutputStream()) //
								.setExecutorService(this.executorService) //
//								.traceMessages(new PrintWriter(System.out)) //
								.create() //
								.startListening();
						this.rollbacks.add(rollback);
					} catch (final IOException exception) {
						throw new RuntimeException(exception);
					}
				});
			} catch (final Exception exception) {
				FlixLogger.logError(exception);
			}
		}
	}

	@Override
	public void close() {
		this.done = true;
		for (final Rollback rollback : this.rollbacks) {
			rollback.run();
		}
	}

	public static FlixLanguageServerSocketThread createAndStart(FlixLanguageServer server, int port) {
		System.out.println("FlixLanguageServerSocketThread.createAndStart()");

		final FlixLanguageServerSocketThread thread = new FlixLanguageServerSocketThread(server, port);
		thread.start();

		System.out.println("Started language socket thread on port " + port);

		return thread;
	}
}