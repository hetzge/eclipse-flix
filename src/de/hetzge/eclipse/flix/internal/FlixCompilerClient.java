package de.hetzge.eclipse.flix.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.CompletionParams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FlixCompilerClient {

	private final WebSocket webSocket;
	private final Listener listener;

	public FlixCompilerClient(WebSocket webSocket, Listener listener) {
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

	public CompletableFuture<JsonObject> sendComplete(CompletionParams position) {
		final String id = UUID.randomUUID().toString();
		final CompletionStage<JsonObject> responseFuture = this.listener.startRequestResponse(id).thenApply(jsonObject -> jsonObject.get("result").getAsJsonObject());
		final JsonObject jsonObject = GsonUtils.getGson().toJsonTree(position).getAsJsonObject();
		jsonObject.addProperty("request", "lsp/complete");
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
		final Listener listener = new Listener();
		final WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:8112"), listener).join();
		return new FlixCompilerClient(webSocket, listener);
	}

	private static class Listener implements WebSocket.Listener {

		private final StringBuilder builder;
		private final Map<String, CompletableFuture<JsonObject>> messagesById; // memory leak

		public Listener() {
			this.builder = new StringBuilder();
			this.messagesById = new ConcurrentHashMap<>();
		}

		public CompletionStage<JsonObject> startRequestResponse(String id) {
			System.out.println("Start request/response with id " + id);
			final CompletableFuture<JsonObject> future = new CompletableFuture<JsonObject>().thenApply(message -> {
				this.messagesById.remove(id);
				return message;
			});
			this.messagesById.put(id, future);
			return future;
		}

		@Override
		public void onOpen(WebSocket webSocket) {
			System.out.println("onOpen using subprotocol " + webSocket.getSubprotocol());
			WebSocket.Listener.super.onOpen(webSocket);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			System.out.println("[FLIX LSP SOCKET (" + last + ")]::" + data);

			this.builder.append(data);
			if (last) {
				final String message = this.builder.toString().replaceAll("\\\\\"", "aaa");
				System.out.println(message);
				final JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
				final String id = jsonObject.get("id").getAsString();
				Optional.ofNullable(this.messagesById.get(id)).ifPresent(future -> {
					System.out.println("Complete request/response with id " + id);
					future.complete(jsonObject);
				});
				this.builder.setLength(0);
			}
			return WebSocket.Listener.super.onText(webSocket, data, last);
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			System.out.println("Bad day! " + webSocket.toString());
			error.printStackTrace();
			WebSocket.Listener.super.onError(webSocket, error);
		}
	}

}
