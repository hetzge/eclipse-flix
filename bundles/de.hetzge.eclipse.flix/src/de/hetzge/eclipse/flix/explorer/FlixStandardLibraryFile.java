package de.hetzge.eclipse.flix.explorer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.TreePath;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.utils.FlixUtils;

public final class FlixStandardLibraryFile {

	private final Path path;

	public FlixStandardLibraryFile(Path path) {
		this.path = path;
	}

	public boolean hasChildren() {
		try {
			return Files.isDirectory(this.path) && Files.list(this.path).findFirst().isPresent();
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	public Object[] getChildren() {
		try {
			return Files.isDirectory(this.path) ? Files.list(this.path).map(FlixStandardLibraryFile::new).collect(Collectors.toList()).toArray() : null;
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	public Object getParent() {
		try {
			final Path parentPath = this.path.getParent();
			if (parentPath == null || !Files.exists(parentPath)) {
				return null;
			}
			final List<FlixVersion> usedFlixVersions = Flix.get().getModel().getUsedFlixVersions();
			for (final FlixVersion version : usedFlixVersions) {
				if (Files.isSameFile(FlixUtils.loadFlixLibraryFolderPath(version, null), parentPath)) {
					return new FlixStandardLibraryRoot(version);
				}
			}
			return new FlixStandardLibraryFile(parentPath);
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	public TreePath getTreePath() {
		final List<Object> segments = new ArrayList<>();
		final Object parent = getParent();
		if (parent instanceof FlixStandardLibraryFile) {
			final FlixStandardLibraryFile parentStandardLibraryFile = (FlixStandardLibraryFile) parent;
			final TreePath parentTreePath = parentStandardLibraryFile.getTreePath();
			for (int i = 0; i < parentTreePath.getSegmentCount(); i++) {
				segments.add(parentTreePath.getSegment(i));
			}
		} else if (parent instanceof FlixStandardLibraryRoot) {
			segments.add(parent);
		} else if (parent == null) {
			FlixLogger.logInfo("Skip standard library entry that no longer exists.");
		} else {
			throw new IllegalStateException("Unknown flix standard library tree element: " + parent);
		}
		segments.add(this);
		return new TreePath(segments.toArray());
	}

	public String getName() {
		return this.path.getFileName().toString();
	}

	public Path getPath() {
		return this.path;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.path);
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
		final FlixStandardLibraryFile other = (FlixStandardLibraryFile) obj;
		try {
			return Files.isSameFile(this.path, other.path);
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public String toString() {
		return "FlixStandardLibraryFile [path=" + this.path + "]";
	}
}
