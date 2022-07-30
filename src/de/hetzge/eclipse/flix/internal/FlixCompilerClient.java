package de.hetzge.eclipse.flix.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class FlixCompilerClient {

	private WebSocket webSocket;

	private void init() {
		this.webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:8112"), new Listener()).join();
	}

	public static synchronized FlixCompilerClient connect() {
		final FlixCompilerClient client = new FlixCompilerClient();
		client.init();
		return client;
	}

	public CompletableFuture<Void> sendAddUri(URI uri, String src) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("request", "api/addUri");
		jsonObject.addProperty("id", UUID.randomUUID().toString());
		jsonObject.addProperty("uri", uri.toString());
		jsonObject.addProperty("src", src);
		return send(jsonObject);
	}

	private CompletableFuture<Void> send(final JsonObject jsonObject) {
		final String jsonString = toJsonString(jsonObject);
		System.out.println("Send: " + jsonString);
		return this.webSocket.sendText(jsonString, true).thenRun(() -> {});
	}

	private String toJsonString(final JsonObject jsonObject) {
		return new GsonBuilder().create().toJson(jsonObject);
	}

	private class Listener implements WebSocket.Listener {

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
