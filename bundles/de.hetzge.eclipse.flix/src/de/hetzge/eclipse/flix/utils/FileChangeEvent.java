package de.hetzge.eclipse.flix.utils;

import java.net.URI;
import java.util.List;

public final class FileChangeEvent {
	private final List<URI> files;

	public FileChangeEvent(List<URI> files) {
		this.files = files;
	}

	public final List<URI> getFiles() {
		return this.files;
	}
}
