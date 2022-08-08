package de.hetzge.eclipse.flix.compiler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import com.google.gson.JsonObject;

import de.hetzge.eclipse.flix.GsonUtils;

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
		this.rollback.reset();
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

	public CompletableFuture<Void> sendJar(URI uri, String base64String) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addJar");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		jsonObject.addProperty("base64", base64String);
		return send(jsonObject);
	}

	public CompletableFuture<Void> sendRemoveJar(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remJar");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		return send(jsonObject);
	}

	public CompletableFuture<Either<JsonObject, JsonObject>> sendComplete(CompletionParams position) {
		final String id = UUID.randomUUID().toString();

		final JsonObject jsonObject = GsonUtils.getGson().toJsonTree(position).getAsJsonObject();
		jsonObject.addProperty("request", "lsp/complete");
		jsonObject.addProperty("id", id);

		final CompletionStage<Either<JsonObject, JsonObject>> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<Either<JsonObject, JsonObject>> sendGoto(DeclarationParams params) {
		final String id = UUID.randomUUID().toString();

		final JsonObject jsonObject = GsonUtils.getGson().toJsonTree(params).getAsJsonObject();
		jsonObject.addProperty("request", "lsp/goto");
		jsonObject.addProperty("id", id);

		final CompletionStage<Either<JsonObject, JsonObject>> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<Either<JsonObject, JsonObject>> sendCheck() {
		final String id = UUID.randomUUID().toString();
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "lsp/check");
		jsonObject.addProperty("id", id);

		final CompletionStage<Either<JsonObject, JsonObject>> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<Either<JsonObject, JsonObject>> sendHover(HoverParams params) {
		final String id = UUID.randomUUID().toString();

		final JsonObject positionJsonObject = new JsonObject();
		positionJsonObject.addProperty("line", params.getPosition().getLine());
		positionJsonObject.addProperty("character", params.getPosition().getCharacter());

		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "lsp/hover");
		jsonObject.addProperty("id", id);
		jsonObject.addProperty("uri", params.getTextDocument().getUri());
		jsonObject.add("position", positionJsonObject);

		final CompletableFuture<Either<JsonObject, JsonObject>> responseFuture = this.listener.startRequestResponse(id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	private CompletableFuture<Void> send(final JsonObject jsonObject) {
		return send(GsonUtils.getGson().toJson(jsonObject));
	}

	private CompletableFuture<Void> send(final String jsonString) {
		System.out.println("Send: " + ", length=" + jsonString.length());

		if (this.webSocket.isInputClosed() || this.webSocket.isOutputClosed()) {
			System.out.println("CLOSED !!!");
			throw new RuntimeException("!!!!!!!");
		} else {
			System.out.println("NOT CLOSED !!!");
		}

		return this.webSocket.sendText(jsonString, true).thenRun(() -> {
			System.out.println("Closed after: " + (this.webSocket.isInputClosed() || this.webSocket.isOutputClosed()));
		});
	}

	public static synchronized FlixCompilerClient connect(int port) {
		System.out.println("FlixCompilerClient.connect()");
		return SafeRun.runWithResult(rollback -> {
			final FlixCompilerProcessSocketListener listener = new FlixCompilerProcessSocketListener();
			final WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port), listener).join();
			rollback.add(webSocket::abort);

			System.out.println("Connected compiler client on port " + port);

			return new FlixCompilerClient(webSocket, listener, rollback);
		});
	}
}