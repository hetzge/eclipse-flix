package de.hetzge.eclipse.flix.internal;

import java.net.http.WebSocket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class FlixCompilerProcessSocketListener implements WebSocket.Listener {

	private final StringBuilder builder;
	private final Map<String, CompletableFuture<JsonObject>> messagesById; // memory leak

	public FlixCompilerProcessSocketListener() {
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
			final String message = this.builder.toString();
			System.out.println(message);
			final JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
			final String id = jsonObject.get("id").getAsString();
			Optional.ofNullable(this.messagesById.get(id)).ifPresent(future -> {
				System.out.println("Complete request/response with id " + id);
				System.out.println("...... " + jsonObject);
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