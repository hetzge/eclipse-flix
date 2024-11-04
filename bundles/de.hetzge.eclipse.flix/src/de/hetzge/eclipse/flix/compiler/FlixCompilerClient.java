package de.hetzge.eclipse.flix.compiler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import com.google.gson.JsonObject;

import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.utils.GsonUtils;

public class FlixCompilerClient implements AutoCloseable {
	private static final ILog LOG = Platform.getLog(FlixCompilerClient.class);

	private final ScheduledThreadPoolExecutor executor;
	private final WebSocket webSocket;
	private final FlixCompilerProcessSocketListener listener;
	private final Rollback rollback;

	public FlixCompilerClient(WebSocket webSocket, FlixCompilerProcessSocketListener listener, Rollback rollback) {
		this.executor = new ScheduledThreadPoolExecutor(1);
		this.webSocket = webSocket;
		this.listener = listener;
		this.rollback = rollback;
	}

	@Override
	public void close() {
		this.rollback.run();
		this.executor.shutdown();
	}

	public CompletableFuture<Void> sendAddUri(URI uri, String src) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addUri"); //$NON-NLS-1$ //$NON-NLS-2$
		jsonObject.addProperty("id", UUID.randomUUID().toString()); //$NON-NLS-1$
		jsonObject.addProperty("uri", uri.toString()); //$NON-NLS-1$
		jsonObject.addProperty("src", src); //$NON-NLS-1$
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendRemoveUri(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remUri"); //$NON-NLS-1$
		jsonObject.addProperty("id", UUID.randomUUID().toString()); //$NON-NLS-1$
		jsonObject.addProperty("uri", uri.toString()); //$NON-NLS-1$
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendFpkg(URI uri, String base64String) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addPkg"); //$NON-NLS-1$ //$NON-NLS-2$
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString()); //$NON-NLS-1$
		jsonObject.addProperty("base64", base64String); //$NON-NLS-1$
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendRemoveFpkg(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remPkg"); //$NON-NLS-1$ //$NON-NLS-2$
		jsonObject.addProperty("id", UUID.randomUUID().toString()); //$NON-NLS-1$
		jsonObject.addProperty("uri", uri.toString()); //$NON-NLS-1$
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendJar(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addJar"); //$NON-NLS-1$ //$NON-NLS-2$
		jsonObject.addProperty("id", UUID.randomUUID().toString()); //$NON-NLS-1$
		jsonObject.addProperty("uri", uri.toString()); //$NON-NLS-1$
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendRemoveJar(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remJar"); //$NON-NLS-1$ //$NON-NLS-2$
		jsonObject.addProperty("id", UUID.randomUUID().toString()); //$NON-NLS-1$
		jsonObject.addProperty("uri", uri.toString()); //$NON-NLS-1$
		return send(jsonObject);
	}

	public CompletableFuture<FlixCompilerResponse> sendComplete(CompletionParams position) {
		return send("lsp/complete", position); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> sendGoto(DeclarationParams params) {
		return send("lsp/goto", params); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> sendCheck() {
		return send("lsp/check", new Object()); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> sendHover(HoverParams params) {
		final String id = UUID.randomUUID().toString();

		final JsonObject positionJsonObject = new JsonObject();
		positionJsonObject.addProperty("line", params.getPosition().getLine()); //$NON-NLS-1$
		positionJsonObject.addProperty("character", params.getPosition().getCharacter()); //$NON-NLS-1$

		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "lsp/hover"); //$NON-NLS-1$ //$NON-NLS-2$
		jsonObject.addProperty("id", id); //$NON-NLS-1$
		jsonObject.addProperty("uri", params.getTextDocument().getUri()); //$NON-NLS-1$
		jsonObject.add("position", positionJsonObject); //$NON-NLS-1$

		final CompletableFuture<FlixCompilerResponse> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<FlixCompilerResponse> sendDocumentSymbols(URI uri) {
		return sendDocumentSymbols(uri.toString());
	}

	public CompletableFuture<FlixCompilerResponse> sendDocumentSymbols(String uri) {
		final String id = UUID.randomUUID().toString();

		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "lsp/documentSymbols"); //$NON-NLS-1$ //$NON-NLS-2$
		jsonObject.addProperty("id", id); //$NON-NLS-1$
		jsonObject.addProperty("uri", uri); //$NON-NLS-1$

		final CompletableFuture<FlixCompilerResponse> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<FlixCompilerResponse> sendDocumentHighlight(DocumentHighlightParams params) {
		return send("lsp/highlight", params); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> sendWorkspaceSymbols(WorkspaceSymbolParams params) {
		return send("lsp/workspaceSymbols", params); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> sendRename(RenameParams params) {
		return send("lsp/rename", params); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> sendUses(ReferenceParams params) {
		return send("lsp/uses", params); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> sendCodeLens(CodeLensParams params) {
		return send("lsp/codelens", params); //$NON-NLS-1$
	}

	public CompletableFuture<FlixCompilerResponse> send(String request, Object params) {
		final String id = UUID.randomUUID().toString();

		final JsonObject jsonObject = GsonUtils.getGson().toJsonTree(params).getAsJsonObject();
		jsonObject.addProperty("request", request); //$NON-NLS-1$
		jsonObject.addProperty("id", id); //$NON-NLS-1$

		final CompletableFuture<FlixCompilerResponse> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	private CompletableFuture<Void> send(final JsonObject jsonObject) {
		return send(GsonUtils.getGson().toJson(jsonObject));
	}

	private CompletableFuture<Void> send(final String jsonString) {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		this.executor.submit(() -> {
			this.webSocket.sendText(jsonString, true).handle((socket, exception) -> {
				if (exception != null) {
					future.completeExceptionally(exception);
				} else {
					future.complete(null);
				}
				return null;
			});
		});
		return future;
	}

	public static synchronized FlixCompilerClient connect(int port) {
		return SafeRun.runWithResult(rollback -> {
			rollback.setLogger(FlixLogger::logError);
			final FlixCompilerProcessSocketListener listener = new FlixCompilerProcessSocketListener();
			final WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port), listener).join(); //$NON-NLS-1$
			rollback.add(webSocket::abort);
			LOG.info("Connected compiler client on port " + port); //$NON-NLS-1$
			return new FlixCompilerClient(webSocket, listener, rollback);
		});
	}
}
