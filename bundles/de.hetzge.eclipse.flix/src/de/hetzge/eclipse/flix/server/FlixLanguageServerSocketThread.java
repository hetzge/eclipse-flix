package de.hetzge.eclipse.flix.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.lxtk.util.SafeRun;

import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.model.FlixProject;

public final class FlixLanguageServerSocketThread extends Thread implements AutoCloseable {
	private final FlixProject flixProject;
	private final int port;
	private final ExecutorService executorService;
	private Status status;

	public FlixLanguageServerSocketThread(FlixProject flixProject, int port) {
		super("LSP Server Socket");
		this.flixProject = flixProject;
		this.port = port;
		this.executorService = Executors.newWorkStealingPool();
		this.status = Status.STOPPED;
		setDaemon(true);
	}

	@Override
	public void run() {
		SafeRun.run(rollback -> {
			this.status = Status.STARTING;
			updateStatus(Status.STARTING);
			try (final ServerSocket serverSocket = new ServerSocket(this.port)) {
				try {
					final Socket socket = serverSocket.accept();
					rollback.add(() -> {
						try {
							socket.close();
						} catch (final IOException exception) {
							FlixLogger.logError(exception);
						}
					});
					final FlixLanguageServer server = FlixLanguageServer.start(this.flixProject);
					rollback.add(server::close);
					final Launcher<LanguageClient> launcher = new Builder<LanguageClient>()
							.setLocalService(server)
							.setRemoteInterface(LanguageClient.class)
							.setInput(socket.getInputStream())
							.setOutput(socket.getOutputStream())
							.setExecutorService(this.executorService)
//								.traceMessages(new PrintWriter(System.out))
							.create();
					server.setClient(launcher.getRemoteProxy());
					final Future<Void> startListeningFuture = launcher.startListening();
					rollback.add(() -> startListeningFuture.cancel(true));
					while (this.status != Status.STOPPED && server.isRunning() && !startListeningFuture.isDone() && !startListeningFuture.isCancelled()) {
						if (server.isInitialized()) {
							updateStatus(Status.STARTED);
						}
						Thread.sleep(1000);
					}
					rollback.run();
				} catch (final IOException | InterruptedException exception) {
					throw new RuntimeException(exception);
				}
			} catch (final Exception exception) {
				FlixLogger.logError(exception);
			} finally {
				updateStatus(Status.STOPPED);
			}
		});
	}

	private void updateStatus(Status status) {
		this.status = status;
	}

	@Override
	public void close() {
		updateStatus(Status.STOPPED);
		interrupt();
	}

	public Status getStatus() {
		return this.status;
	}

	public static FlixLanguageServerSocketThread createAndStart(FlixProject project, int port) {
		final FlixLanguageServerSocketThread thread = new FlixLanguageServerSocketThread(project, port);
		thread.start();
		return thread;
	}

	public enum Status {
		STARTING, STARTED, STOPPED;
	}
}