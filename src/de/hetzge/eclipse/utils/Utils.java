package de.hetzge.eclipse.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public final class Utils {
	private Utils() {
	}

	public static String readUriBase64Encoded(URI uri) {
		try (InputStream inputStream = uri.toURL().openStream()) {
			return new String(Base64.getEncoder().encode(inputStream.readAllBytes()), StandardCharsets.US_ASCII);
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static String readFileContent(IFile file) throws IOException, CoreException {
		return new String(file.getContents().readAllBytes(), StandardCharsets.UTF_8);
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

}
