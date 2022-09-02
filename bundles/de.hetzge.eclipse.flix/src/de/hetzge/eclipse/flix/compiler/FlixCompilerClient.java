package de.hetzge.eclipse.flix.compiler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.utils.GsonUtils;

public class FlixCompilerClient implements AutoCloseable {

	private final WebSocket webSocket;
	private final FlixCompilerProcessSocketListener listener;
	private final Rollback rollback;

	public FlixCompilerClient(WebSocket webSocket, FlixCompilerProcessSocketListener listener, Rollback rollback) {
		this.webSocket = webSocket;
		this.listener = listener;
		this.rollback = rollback;
	}

	@Override
	public void close() {
		this.rollback.run();
	}

	public CompletableFuture<Void> sendAddUri(URI uri, String src) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addUri");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		jsonObject.addProperty("src", src);
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendRemoveUri(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remUri");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendFpkg(URI uri, String base64String) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addPkg");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		jsonObject.addProperty("base64", base64String);
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendRemoveFpkg(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remPkg");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendJar(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addJar");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendRemoveJar(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remJar");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		return send(jsonObject);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendComplete(CompletionParams position) {
		return send("lsp/complete", position);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendGoto(DeclarationParams params) {
		return send("lsp/goto", params);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendCheck() {
		return send("lsp/check", new Object());
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendHover(HoverParams params) {
		final String id = UUID.randomUUID().toString();

		final JsonObject positionJsonObject = new JsonObject();
		positionJsonObject.addProperty("line", params.getPosition().getLine());
		positionJsonObject.addProperty("character", params.getPosition().getCharacter());

		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "lsp/hover");
		jsonObject.addProperty("id", id);
		jsonObject.addProperty("uri", params.getTextDocument().getUri());
		jsonObject.add("position", positionJsonObject);

		final CompletableFuture<Either<JsonElement, JsonElement>> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendDocumentSymbols(URI uri) {
		final String id = UUID.randomUUID().toString();

		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "lsp/documentSymbols");
		jsonObject.addProperty("id", id);
		jsonObject.addProperty("uri", uri.toASCIIString());

		final CompletableFuture<Either<JsonElement, JsonElement>> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendWorkspaceSymbols(WorkspaceSymbolParams params) {
		return send("lsp/workspaceSymbols", params);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendRename(RenameParams params) {
		return send("lsp/rename", params);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendUses(ReferenceParams params) {
		return send("lsp/uses", params);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> sendCodeLens(CodeLensParams params) {
		return send("lsp/codelens", params);
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> send(String request, Object params) {
		final String id = UUID.randomUUID().toString();

		final JsonObject jsonObject = GsonUtils.getGson().toJsonTree(params).getAsJsonObject();
		jsonObject.addProperty("request", request);
		jsonObject.addProperty("id", id);

		final CompletableFuture<Either<JsonElement, JsonElement>> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	private CompletableFuture<Void> send(final JsonObject jsonObject) {
		return send(GsonUtils.getGson().toJson(jsonObject));
	}

	private CompletableFuture<Void> send(final String jsonString) {
		System.out.println("Send with length " + jsonString.length());
		return this.webSocket.sendText(jsonString, true).thenRun(() -> {
		});
	}

	public static synchronized FlixCompilerClient connect(int port) {
		System.out.println("FlixCompilerClient.connect()");
		return SafeRun.runWithResult(rollback -> {
			rollback.setLogger(FlixLogger::logError);
			
			final FlixCompilerProcessSocketListener listener = new FlixCompilerProcessSocketListener();
			final WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port), listener).join();
			rollback.add(webSocket::abort);

			System.out.println("Connected compiler client on port " + port);

			return new FlixCompilerClient(webSocket, listener, rollback);
		});
	}
}
