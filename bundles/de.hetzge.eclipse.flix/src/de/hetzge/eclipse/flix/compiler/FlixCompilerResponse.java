package de.hetzge.eclipse.flix.compiler;

import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonElement;

public final class FlixCompilerResponse {

	private final JsonElement successJsonElement;
	private final JsonElement failureJsonElement;

	public FlixCompilerResponse(JsonElement successJsonElement, JsonElement failureJsonElement) {
		this.successJsonElement = successJsonElement;
		this.failureJsonElement = failureJsonElement;
	}

	public Optional<JsonElement> getSuccessJsonElement() {
		return Optional.ofNullable(this.successJsonElement);
	}

	public Optional<JsonElement> getFailureJsonElement() {
		return Optional.ofNullable(this.failureJsonElement);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.failureJsonElement, this.successJsonElement);
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
		return Objects.equals(this.failureJsonElement, other.failureJsonElement) && Objects.equals(this.successJsonElement, other.successJsonElement);
	}

	@Override
	public String toString() {
		return "FlixCompilerResponse [successJsonElement=" + this.successJsonElement + ", failureJsonElement=" + this.failureJsonElement + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
