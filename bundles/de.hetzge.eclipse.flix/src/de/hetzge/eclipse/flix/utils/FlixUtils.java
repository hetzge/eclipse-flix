package de.hetzge.eclipse.flix.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

import de.hetzge.eclipse.utils.EclipseUtils;

public final class FlixUtils {
	private FlixUtils() {
	}

	public synchronized static File loadFlixJarFile() {
		final File flixJarFile = new File("flix.v0.30.0.jar");
		if (!flixJarFile.exists()) {
			System.out.println("Download flix.jar");
			try (FileOutputStream outputStream = new FileOutputStream(flixJarFile)) {
				URI.create("https://github.com/flix/flix/releases/download/v0.30.0/flix.jar").toURL().openStream().transferTo(outputStream);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixJarFile;
	}

	public synchronized static File loadFlixFolder() {
		final File flixSourceFolder = new File("flix.v0.30.0");
		final File flixJarFile = loadFlixJarFile();
		if (!flixSourceFolder.exists()) {
			try {
				extract(flixJarFile, flixSourceFolder);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixSourceFolder;
	}

	private static void extract(final File archiveFile, final File flixSourceFolder) throws IOException {
		try (ArchiveInputStream inputStream = new JarArchiveInputStream(new BufferedInputStream(Files.newInputStream(archiveFile.toPath())))) {
			ArchiveEntry entry = null;
			while ((entry = inputStream.getNextEntry()) != null) {
				if (!inputStream.canReadEntryData(entry)) {
					// log something?
					continue;
				}
				final File file = new File(flixSourceFolder, entry.getName());
				if (entry.isDirectory()) {
					if (!file.isDirectory() && !file.mkdirs()) {
						throw new IOException("failed to create directory " + file);
					}
				} else {
					final File parent = file.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("failed to create directory " + parent);
					}
					try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
						IOUtils.copy(inputStream, outputStream);
					}
				}
			}
		}
	}

	public static List<IFile> findFlixFiles(IContainer container) {
		final List<IFile> files = new ArrayList<>();
		EclipseUtils.visitFiles(container, file -> {
			if (isFlixFile(file)) {
				files.add(file);
			}
		});
		return files;
	}

	private static boolean isFlixFile(IFile file) {
		return file.getFileExtension() != null && (file.getFileExtension().equals("flix") || file.getFileExtension().equals("fpkg"));
	}

}
