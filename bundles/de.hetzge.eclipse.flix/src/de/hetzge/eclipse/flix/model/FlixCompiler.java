package de.hetzge.eclipse.flix.model;

import java.io.File;
import java.util.Objects;

import de.hetzge.eclipse.flix.core.model.FlixVersion;

public final class FlixCompiler {
	private final FlixVersion version;
	private final File compilerFile;
	private final File compilerFolder;

	public FlixCompiler(FlixVersion version, File compilerFile, File compilerFolder) {
		this.version = version;
		this.compilerFile = compilerFile;
		this.compilerFolder = compilerFolder;
	}

	public FlixVersion getVersion() {
		return this.version;
	}

	public File getCompilerFile() {
		return this.compilerFile;
	}

	public File getCompilerFolder() {
		return this.compilerFolder;
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
		final FlixCompiler other = (FlixCompiler) obj;
		return Objects.equals(this.version, other.version);
	}

	@Override
	public String toString() {
		return "FlixCompiler [version=" + this.version + ", compilerFile=" + this.compilerFile + ", compilerFolder=" + this.compilerFolder + "]";
	}
}
