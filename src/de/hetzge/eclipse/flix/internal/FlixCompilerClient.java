package de.hetzge.eclipse.flix.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.lsp4j.CompletionParams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class FlixCompilerClient {

	private final WebSocket webSocket;
	private final Gson gson;

	public FlixCompilerClient(WebSocket webSocket) {
		this.webSocket = webSocket;
		this.gson = new GsonBuilder().create();
	}

	public CompletableFuture<Void> sendAddUri(URI uri, String src) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addUri");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		jsonObject.addProperty("src", src);
		return send(this.gson.toJson(jsonObject));
	}

	public CompletableFuture<Void> sendRemoveUri(URI uri) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/remUri");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		return send(this.gson.toJson(jsonObject));
	}

	public CompletableFuture<Void> sendComplete(CompletionParams position) {
		return send(this.gson.toJson(position));
	}

	private CompletableFuture<Void> send(final String jsonString) {
		System.out.println("Send: " + jsonString);
		return this.webSocket.sendText(jsonString, true).thenRun(() -> {
		});
	}

	public static synchronized FlixCompilerClient connect() {
		final WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:8112"), new Listener()).join();
		return new FlixCompilerClient(webSocket);
	}

	private static class Listener implements WebSocket.Listener {

		@Override
		public void onOpen(WebSocket webSocket) {
			System.out.println("onOpen using subprotocol " + webSocket.getSubprotocol());
			WebSocket.Listener.super.onOpen(webSocket);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			System.out.println("[FLIX LSP SOCKET]::" + data);
			return WebSocket.Listener.super.onText(webSocket, data, last);
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			System.out.println("Bad day! " + webSocket.toString());
			WebSocket.Listener.super.onError(webSocket, error);
		}
	}

}
