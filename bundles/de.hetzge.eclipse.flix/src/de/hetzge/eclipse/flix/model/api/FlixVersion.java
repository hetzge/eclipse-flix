package de.hetzge.eclipse.flix.model.api;

import java.util.List;
import java.util.Objects;

public final class FlixVersion {

	public final static FlixVersion VERSION_0_30_0 = new FlixVersion("v0.30.0");
	public final static FlixVersion VERSION_0_31_0 = new FlixVersion("v0.31.0");
	public final static FlixVersion VERSION_0_32_0 = new FlixVersion("v0.32.0");
	public final static FlixVersion CUSTOM = new FlixVersion("<project>/flix.jar");
	public final static List<FlixVersion> VERSIONS = List.of(VERSION_0_30_0, VERSION_0_31_0, VERSION_0_32_0);

	private final String key;

	public FlixVersion(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.key);
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
		final FlixVersion other = (FlixVersion) obj;
		return Objects.equals(this.key, other.key);
	}

	@Override
	public String toString() {
		return "FlixVersion [key=" + this.key + "]";
	}
}
