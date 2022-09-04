package de.hetzge.eclipse.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public final class Utils {
	private Utils() {
	}

	public static <T> List<T> toList(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
	}

	public static String readUriBase64Encoded(URI uri) {
		try (InputStream inputStream = uri.toURL().openStream()) {
			return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static String readFileContent(IFile file) {
		try {
			return new String(file.getContents().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException | CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static File getJreExecutable() {
		final String jreDirectory = System.getProperty("java.home");
		if (jreDirectory == null) {
			throw new IllegalStateException("'java.home' not set");
		}
		File exe;
		if (isWindows()) {
			exe = new File(jreDirectory, "bin/java.exe");
		} else {
			exe = new File(jreDirectory, "bin/java");
		}
		if (!exe.isFile()) {
			throw new IllegalStateException("Java not found under '" + exe.getAbsolutePath() + "'");
		}
		return exe;
	}

	private static boolean isWindows() {
		String os = System.getProperty("os.name");
		if (os == null) {
			throw new IllegalStateException("os.name");
		}
		os = os.toLowerCase();
		return os.startsWith("windows");
	}

	public synchronized static int queryPort() {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static void deleteFolder(File file) {
		final File[] contents = file.listFiles();
		if (contents != null) {
			for (final File f : contents) {
				if (!Files.isSymbolicLink(f.toPath())) {
					deleteFolder(f);
				}
			}
		}
		file.delete();
	}

	public static URL createUrl(String value) {
		try {
			return new URL(value);
		} catch (final MalformedURLException exception) {
			throw new RuntimeException(exception);
		}
	}

}
