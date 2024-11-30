package de.hetzge.eclipse.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

	public static int secondLastIndex(String value, String find) {
		if (value.lastIndexOf(find) == -1) {
			return -1;
		}
		return value.substring(0, value.lastIndexOf(find)).lastIndexOf(find);
	}

	public static void waitForProcess(Process process) {
		while (process.isAlive()) {
			try {
				Thread.sleep(100L);
			} catch (final InterruptedException exception) {
				break;
			}
		}
	}

	public static String md5(File file) {
		try (DigestInputStream digestInputStream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"))) {
			while (digestInputStream.read() != -1) {
			}
			return new BigInteger(1, digestInputStream.getMessageDigest().digest()).toString(16);
		} catch (final IOException | NoSuchAlgorithmException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static String md5(String input) {
		try {
			return new BigInteger(1, MessageDigest.getInstance("MD5").digest(input.getBytes())).toString(16);
		} catch (final NoSuchAlgorithmException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static boolean deleteDirectory(File directoryToBeDeleted) {
		final File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (final File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

}
