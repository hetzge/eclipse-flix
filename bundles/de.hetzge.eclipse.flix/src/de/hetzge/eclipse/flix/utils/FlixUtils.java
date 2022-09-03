package de.hetzge.eclipse.flix.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import de.hetzge.eclipse.utils.Utils;

public final class FlixUtils {
	private FlixUtils() {
	}

	public synchronized static File loadFlixJarFile(String version, IProgressMonitor monitor) {
		final String flixJarName = "flix." + version + ".jar";
		final File flixJarFile = new File(flixJarName);
		if (!flixJarFile.exists()) {
			System.out.println("Download " + flixJarName);
			try (final FileOutputStream outputStream = new FileOutputStream(flixJarFile)) {
				final URL url = URI.create("https://github.com/flix/flix/releases/download/" + version + "/flix.jar").toURL();
				final HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
				final long completeFileSize = httpConnection.getContentLength();
				final SubMonitor subMonitor;
				if (monitor != null) {
					subMonitor = SubMonitor.convert(monitor, (int) completeFileSize);
					subMonitor.setTaskName("Download " + flixJarName);
				} else {
					subMonitor = null;
				}
				final BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
				int read = 0;
				final byte[] buffer = new byte[1024];
				while ((read = inputStream.read(buffer)) >= 0) {
					outputStream.write(buffer, 0, read);
					if (subMonitor != null) {
						subMonitor.worked(read);
					}
				}
				httpConnection.getInputStream().transferTo(outputStream);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixJarFile;
	}

	public synchronized static File loadFlixFolder(String version, IProgressMonitor monitor) {
		final File flixSourceFolder = new File("flix." + version);
		final File flixJarFile = loadFlixJarFile(version, monitor);
		if (!flixSourceFolder.exists()) {
			try {
				extract(flixJarFile, flixSourceFolder, monitor);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixSourceFolder;
	}

	private static void extract(File archiveFile, File flixSourceFolder, IProgressMonitor monitor) throws IOException {
		System.out.println("FlixUtils.extract(" + archiveFile.getAbsolutePath() + ", " + flixSourceFolder.getAbsolutePath() + ")");
		try (ArchiveInputStream inputStream = new JarArchiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile)))) {
			SubMonitor subMonitor;
			if (monitor != null) {
				subMonitor = SubMonitor.convert(monitor, IProgressMonitor.UNKNOWN);
				subMonitor.setTaskName("Extract " + archiveFile.getAbsolutePath());
			} else {
				subMonitor = null;
			}
			ArchiveEntry entry = null;
			while ((entry = inputStream.getNextEntry()) != null) {
				if (inputStream.canReadEntryData(entry)) {
					if (subMonitor != null) {
						final SubMonitor subSubMonitor = subMonitor.setWorkRemaining(100).split(1);
						subSubMonitor.setTaskName("Extract " + entry.getName());
						subSubMonitor.worked(1);
					}
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
		} catch (final Exception exception) {
			// Rollback already created files
			Utils.deleteFolder(flixSourceFolder);
			throw exception;
		}
	}

}
