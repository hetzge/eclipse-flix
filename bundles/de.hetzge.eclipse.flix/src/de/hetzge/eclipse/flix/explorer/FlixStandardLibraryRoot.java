package de.hetzge.eclipse.flix.explorer;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.utils.FlixUtils;

public final class FlixStandardLibraryRoot {
	private final FlixVersion version;

	public FlixStandardLibraryRoot(FlixVersion version) {
		this.version = version;
	}

	public boolean hasChildren() {
		return true;
	}

	public Object[] getChildren() {
		final File flixFolder = FlixUtils.loadFlixFolder(this.version, null);
		if (!flixFolder.exists() || !flixFolder.isDirectory()) {
			return null;
		}
		final File libraryFolder = new File(flixFolder, "src/library");
		if (!libraryFolder.exists() || !libraryFolder.isDirectory()) {
			return null;
		}
		return Arrays.asList(libraryFolder.listFiles()).stream().map(FlixStandardLibraryFile::new).toArray();
	}

	public String getName() {
		return String.format("Flix Standard Library %s", this.version.getKey());
	}

	public FlixVersion getVersion() {
		return this.version;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.version);
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
		final FlixStandardLibraryRoot other = (FlixStandardLibraryRoot) obj;
		return Objects.equals(this.version, other.version);
	}

	@Override
	public String toString() {
		return "FlixStandardLibraryRoot [version=" + this.version + "]";
	}
}