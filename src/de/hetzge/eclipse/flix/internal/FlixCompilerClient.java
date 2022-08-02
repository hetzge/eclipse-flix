package de.hetzge.eclipse.flix.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;

import com.google.gson.JsonObject;

public class FlixCompilerClient {

	private final WebSocket webSocket;
	private final FlixCompilerProcessSocketListener listener;

	public FlixCompilerClient(WebSocket webSocket, FlixCompilerProcessSocketListener listener) {
		this.webSocket = webSocket;
		this.listener = listener;
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

	public CompletableFuture<JsonObject> sendComplete(CompletionParams position) {
		final String id = UUID.randomUUID().toString();
		final CompletionStage<JsonObject> responseFuture = this.listener.startRequestResponse(id).thenApply(jsonObject -> jsonObject.get("result").getAsJsonObject());
		final JsonObject jsonObject = GsonUtils.getGson().toJsonTree(position).getAsJsonObject();
		jsonObject.addProperty("request", "lsp/complete");
		jsonObject.addProperty("id", id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	public CompletableFuture<JsonObject> sendGoto(DeclarationParams params) {
		final String id = UUID.randomUUID().toString();
		final CompletionStage<JsonObject> responseFuture = this.listener.startRequestResponse(id).thenApply(jsonObject -> jsonObject.get("result").getAsJsonObject());
		final JsonObject jsonObject = GsonUtils.getGson().toJsonTree(params).getAsJsonObject();
		jsonObject.addProperty("request", "lsp/goto");
		jsonObject.addProperty("id", id);
		return send(jsonObject).thenCompose(ignore -> responseFuture);
	}

	private CompletableFuture<Void> send(final JsonObject jsonObject) {
		return send(GsonUtils.getGson().toJson(jsonObject));
	}

	private CompletableFuture<Void> send(final String jsonString) {
		System.out.println("Send: " + jsonString);
		return this.webSocket.sendText(jsonString, true).thenRun(() -> {
		});
	}

	public static synchronized FlixCompilerClient connect() {
		final FlixCompilerProcessSocketListener listener = new FlixCompilerProcessSocketListener();
		final WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:8112"), listener).join();
		return new FlixCompilerClient(webSocket, listener);
	}
}
