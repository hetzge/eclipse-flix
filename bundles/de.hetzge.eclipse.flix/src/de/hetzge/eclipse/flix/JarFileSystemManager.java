package de.hetzge.eclipse.flix;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JarFileSystemManager implements AutoCloseable {

	private final Map<File, FileSystem> activeFileSystemsByJarUri;

	public JarFileSystemManager() {
		this.activeFileSystemsByJarUri = new ConcurrentHashMap<>();
	}

	public void enableFileSystem(URI uri) {
		final URI jarUri = extractJarUri(uri);
		System.out.println(jarUri);
		this.activeFileSystemsByJarUri.computeIfAbsent(new File(jarUri.toString().replace("jar:", "")), key -> {
			try {
				System.out.println("Enable file system: " + jarUri);
				return FileSystems.newFileSystem(jarUri, Map.of("create", "true"));
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		});
	}

	public void disableFileSystem(URI uri) {
		try {
			final URI jarUri = extractJarUri(uri);
			System.out.println("Disable file system: " + jarUri);
			this.activeFileSystemsByJarUri.remove(new File(jarUri.toString().replace("jar:", ""))).close();
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	private URI extractJarUri(URI uri) {
		final String uriString = uri.toString();
		if (!uriString.startsWith("jar:file:")) {
			throw new IllegalArgumentException("Invalid jar filesystem uri: " + uriString);
		} else if (uriString.endsWith(".jar")) {
			return URI.create(uriString);
		} else if (uriString.contains("!/")) {
			return URI.create(uriString.split("!\\/")[0]);
		} else {
			throw new IllegalArgumentException("Invalid jar filesystem uri: " + uriString);
		}
	}

	@Override
	public void close() {
		this.activeFileSystemsByJarUri.keySet().forEach(file -> disableFileSystem(file.toURI()));
	}
}
