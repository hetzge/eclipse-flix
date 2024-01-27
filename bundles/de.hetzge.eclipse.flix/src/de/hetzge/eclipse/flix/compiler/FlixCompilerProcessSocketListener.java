package de.hetzge.eclipse.flix.compiler;

import java.net.http.WebSocket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class FlixCompilerProcessSocketListener implements WebSocket.Listener {
	private static final ILog LOG = Platform.getLog(FlixCompilerProcessSocketListener.class);

	private static final String SUCCESS_STATUS_VALUE = "success"; //$NON-NLS-1$
	private static final String FAILURE_STATUS_VALUE = "failure"; //$NON-NLS-1$

	private final StringBuilder builder;
	private final Map<String, CompletableFuture<JsonObject>> messagesById; // TODO memory leak

	public FlixCompilerProcessSocketListener() {
		this.builder = new StringBuilder();
		this.messagesById = new ConcurrentHashMap<>();
	}

	public CompletableFuture<FlixCompilerResponse> startRequestResponse(String id) {
		LOG.info("Start flix compiler request with id: " + id); //$NON-NLS-1$
		final CompletableFuture<JsonObject> future = new CompletableFuture<>();
		final CompletableFuture<FlixCompilerResponse> successFailureFuture = future.thenApply(message -> {
			this.messagesById.remove(id);
			final String statusValue = message.get("status").getAsString(); //$NON-NLS-1$
			final JsonElement resultJsonElement = message.get("result");
			if (statusValue.equals(SUCCESS_STATUS_VALUE)) {
				return new FlixCompilerResponse(resultJsonElement, null);
			} else if (statusValue.equals(FAILURE_STATUS_VALUE)) {
				return new FlixCompilerResponse(null, resultJsonElement);
			} else {
				throw new IllegalStateException(String.format("Unexpected status '%s' with message '%s'", statusValue, message.get("message"))); //$NON-NLS-1$
			}
		});
		successFailureFuture.exceptionally(throwable -> {
			LOG.error("Failed request response handling", throwable); //$NON-NLS-1$
			return null;
		});
		this.messagesById.put(id, future);
		return successFailureFuture;
	}

	@Override
	public void onOpen(WebSocket webSocket) {
		WebSocket.Listener.super.onOpen(webSocket);
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		this.builder.append(data);
		if (last) {
			final String message = this.builder.toString();
			final JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
			final String id = jsonObject.get("id").getAsString();
			Optional.ofNullable(this.messagesById.get(id)).ifPresent(future -> {
				LOG.info("Complete request/response with id " + id); //$NON-NLS-1$
				future.complete(jsonObject);
			});
			this.builder.setLength(0);
		}
		return WebSocket.Listener.super.onText(webSocket, data, last);
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		LOG.error("Bad day!", error); //$NON-NLS-1$
		WebSocket.Listener.super.onError(webSocket, error);
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
	}
}