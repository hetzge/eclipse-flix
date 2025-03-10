package de.hetzge.eclipse.flix.compiler;

import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonElement;

public final class FlixCompilerResponse {

	private final JsonElement successJsonElement;
	private final JsonElement failureJsonElement;
	private final boolean invalidRequest;

	public FlixCompilerResponse(JsonElement successJsonElement, JsonElement failureJsonElement, boolean invalidRequest) {
		this.successJsonElement = successJsonElement;
		this.failureJsonElement = failureJsonElement;
		this.invalidRequest = invalidRequest;
	}

	public Optional<JsonElement> getSuccessJsonElement() {
		return Optional.ofNullable(this.successJsonElement);
	}

	public Optional<JsonElement> getFailureJsonElement() {
		return Optional.ofNullable(this.failureJsonElement);
	}

	public boolean isInvalidRequest() {
		return this.invalidRequest;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.failureJsonElement, this.invalidRequest, this.successJsonElement);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final FlixCompilerResponse other = (FlixCompilerResponse) obj;
		return Objects.equals(this.failureJsonElement, other.failureJsonElement) && this.invalidRequest == other.invalidRequest && Objects.equals(this.successJsonElement, other.successJsonElement);
	}

	@Override
	public String toString() {
		return "FlixCompilerResponse [successJsonElement=" + this.successJsonElement + ", failureJsonElement=" + this.failureJsonElement + ", invalidRequest=" + this.invalidRequest + "]";
	}
}
