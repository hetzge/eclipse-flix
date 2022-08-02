package de.hetzge.eclipse;

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
}
