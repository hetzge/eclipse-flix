package de.hetzge.eclipse.flix.compiler;

import java.net.http.WebSocket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.hetzge.eclipse.flix.FlixLogger;

class FlixCompilerProcessSocketListener implements WebSocket.Listener {

	private static final String SUCCESS_STATUS_VALUE = "success";
	private static final String FAILURE_STATUS_VALUE = "failure";

	private final StringBuilder builder;
	private final Map<String, CompletableFuture<JsonObject>> messagesById; // TODO memory leak

	public FlixCompilerProcessSocketListener() {
		this.builder = new StringBuilder();
		this.messagesById = new ConcurrentHashMap<>();
	}

	public CompletableFuture<Either<JsonElement, JsonElement>> startRequestResponse(String id) {
		System.out.println("Start request/response with id " + id);
		final CompletableFuture<JsonObject> future = new CompletableFuture<>();
		final CompletableFuture<Either<JsonElement, JsonElement>> successFailureFuture = future.thenApply(message -> {
			this.messagesById.remove(id);
			final String statusValue = message.get("status").getAsString();
			final JsonElement resultJsonElement = message.get("result");
			if (statusValue.equals(SUCCESS_STATUS_VALUE)) {
				return Either.forLeft(resultJsonElement != null ? resultJsonElement : JsonNull.INSTANCE);
			} else if (statusValue.equals(FAILURE_STATUS_VALUE)) {
				return Either.forRight(resultJsonElement != null ? resultJsonElement : JsonNull.INSTANCE);
			} else {
				throw new IllegalStateException(String.format("Unexpected status '%s'", statusValue));
			}
		});
		successFailureFuture.exceptionally(throwable -> {
			FlixLogger.logError("Failed request response handling", throwable);
			return null;
		});
		this.messagesById.put(id, future);
		return successFailureFuture;
	}

	@Override
	public void onOpen(WebSocket webSocket) {
		System.out.println("onOpen using subprotocol '" + webSocket.getSubprotocol() + "'");
		WebSocket.Listener.super.onOpen(webSocket);
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		System.out.println("[FLIX LSP SOCKET (" + last + ")]::" + data);

		this.builder.append(data);
		if (last) {
			final String message = this.builder.toString();
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

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		System.out.println(String.format("Closed websocket with status code '%s' and reason '%s'", statusCode, reason));
		return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
	}
}