package de.hetzge.eclipse.flix.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

public final class FlixUtils {
	private FlixUtils() {
	}

	public synchronized static File loadFlixJarFile(String version) {
		final String flixJarName = "flix." + version + ".jar";
		final File flixJarFile = new File(flixJarName);
		if (!flixJarFile.exists()) {
			System.out.println("Download " + flixJarName);
			try (FileOutputStream outputStream = new FileOutputStream(flixJarFile)) {
				URI.create("https://github.com/flix/flix/releases/download/" + version + "/flix.jar").toURL().openStream().transferTo(outputStream);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixJarFile;
	}

	public synchronized static File loadFlixFolder(String version) {
		final File flixSourceFolder = new File("flix." + version);
		final File flixJarFile = loadFlixJarFile(version);
		if (!flixSourceFolder.exists()) {
			try {
				extract(flixJarFile, flixSourceFolder);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixSourceFolder;
	}

	public static void main(String[] args) throws IOException {
		FlixUtils.extract(new File("/home/hetzge/apps/eclipsepde/flix.v0.30.0.jar"), new File("/home/hetzge/apps/eclipsepde/flix.v0.30.0"));
	}

	private static void extract(File archiveFile, File flixSourceFolder) throws IOException {
		System.out.println("FlixUtils.extract(" + archiveFile.getAbsolutePath() + ", " + flixSourceFolder.getAbsolutePath() + ")");
		try (ArchiveInputStream inputStream = new JarArchiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile)))) {
			ArchiveEntry entry = null;
			while ((entry = inputStream.getNextEntry()) != null) {
				if (inputStream.canReadEntryData(entry)) {
					if (entry.isDirectory()) {
						final File file = new File(flixSourceFolder, entry.getName());
						if (!file.isDirectory() && !file.mkdirs()) {
							throw new IOException("failed to create directory " + file);
						}
					} else {
						final File file = new File(flixSourceFolder, entry.getName());
						final File parent = file.getParentFile();
						if (!parent.isDirectory() && !parent.mkdirs()) {
							throw new IOException("failed to create directory " + parent);
						}
						try (OutputStream outputStream = new FileOutputStream(file)) {
							IOUtils.copy(inputStream, outputStream);
						}
					}
				} else {
					throw new RuntimeException("Unreadable entry: " + entry);
				}
			}
		}
	}
}
