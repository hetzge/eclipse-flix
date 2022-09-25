package de.hetzge.eclipse.flix.explorer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.TreePath;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.utils.FlixUtils;

public final class FlixStandardLibraryFile {

	private final File file;

	public FlixStandardLibraryFile(File file) {
		this.file = file;
	}

	public boolean hasChildren() {
		if (this.file.isDirectory()) {
			final File[] files = this.file.listFiles();
			return files != null && files.length > 0;
		} else {
			return false;
		}
	}

	public Object[] getChildren() {
		if (!this.file.isDirectory()) {
			return null;
		} else {
			return Arrays.asList(this.file.listFiles()).stream().map(FlixStandardLibraryFile::new).toArray();
		}
	}

	public Object getParent() {
		try {
			final Path parentPath = this.file.getParentFile().toPath();
			if (!Files.exists(parentPath)) {
				return null;
			}
			final List<FlixVersion> usedFlixVersions = Flix.get().getModel().getUsedFlixVersions();
			for (final FlixVersion version : usedFlixVersions) {
				if (Files.isSameFile(FlixUtils.loadFlixLibraryFolder(version, null).toPath(), parentPath)) {
					return new FlixStandardLibraryRoot(version);
				}
			}
			return new FlixStandardLibraryFile(this.file.getParentFile());
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
		return this.file.getName();
	}

	public File getFile() {
		return this.file;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.file);
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
			return Files.isSameFile(this.file.toPath(), other.file.toPath());
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public String toString() {
		return "FlixStandardLibraryFile [file=" + this.file + "]";
	}
}
