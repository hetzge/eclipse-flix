package de.hetzge.eclipse.flix.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FlixVersion {

	public static final FlixVersion VERSION_0_56_0 = new FlixVersion("0.56.0", "dbd8e63de44c5ae59efdf2c5e610b568");
	public static final List<FlixVersion> VERSIONS = List.of(VERSION_0_56_0);
	public static final FlixVersion DEFAULT_VERSION = FlixVersion.VERSION_0_56_0;

	private final String key;

	/**
	 * Can be <code>null</code>
	 */
	private final String checksum;

	public FlixVersion(String key, String checksum) {
		Objects.requireNonNull(key, "'key' is null");
		this.key = key;
		this.checksum = checksum;
	}

	public String getKey() {
		return this.key;
	}

	public String getChecksum() {
		return this.checksum;
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

	public static Optional<FlixVersion> getVersionByName(String versionName) {
		return FlixVersion.VERSIONS.stream().filter(it -> it.getKey().equals(versionName)).findFirst();
	}
}
