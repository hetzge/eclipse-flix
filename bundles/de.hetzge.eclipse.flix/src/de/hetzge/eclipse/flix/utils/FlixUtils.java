package de.hetzge.eclipse.flix.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Objects;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SwtCallable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import de.hetzge.eclipse.flix.core.model.FlixVersion;
import de.hetzge.eclipse.utils.Utils;

public final class FlixUtils {
	private FlixUtils() {
	}

	public synchronized static File loadFlixJarFile(FlixVersion version, IProgressMonitor monitor) {
		final String flixJarName = "flix.v" + version.getKey() + ".jar";
		final File flixJarFile = new File("_flix", flixJarName);
		// delete file if it is corrupt
		if (flixJarFile.exists() && flixJarFile.length() == 0) {
			flixJarFile.delete();
		}
		flixJarFile.getParentFile().mkdirs();
		if (!flixJarFile.exists()) {
			System.out.println("Download " + flixJarName);
			try (final FileOutputStream outputStream = new FileOutputStream(flixJarFile)) {
				final URL url = URI.create("https://github.com/flix/flix/releases/download/v" + version.getKey() + "/flix.jar").toURL();

				confirmDownload(version, url);

				final HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());

				final long completeFileSize = httpConnection.getContentLength();
				final SubMonitor subMonitor;
				if (monitor != null) {
					subMonitor = SubMonitor.convert(monitor, (int) completeFileSize);
					subMonitor.setTaskName("Download " + flixJarName);
				} else {
					subMonitor = null;
				}
				final MessageDigest digest = MessageDigest.getInstance("MD5");
				try (DigestInputStream inputStream = new DigestInputStream(new BufferedInputStream(httpConnection.getInputStream()), digest)) {
					int read = 0;
					final byte[] buffer = new byte[1024];
					while ((read = inputStream.read(buffer)) >= 0) {
						outputStream.write(buffer, 0, read);
						if (subMonitor != null) {
							subMonitor.worked(read);
						}
					}
				} finally {
					if (version.getChecksum() != null) {
						final String md5Checksum = String.format("%032X", new BigInteger(1, digest.digest())).toLowerCase();
						if (!Objects.equals(md5Checksum, version.getChecksum().toLowerCase())) {
							flixJarFile.delete();
							throw new RuntimeException("Downloaded flix file has incorrect MD5 checksum: " + md5Checksum + " (expected: " + version.getChecksum() + ")");
						}
					}
				}
			} catch (final Exception exception) {
				if (flixJarFile.exists()) {
					flixJarFile.delete();
				}
				throw new RuntimeException(exception);
			}
		}
		return flixJarFile;
	}

	private static void confirmDownload(FlixVersion version, URL url) {
		final boolean isUiThread = Thread.currentThread() == Display.getDefault().getThread();
		final Display display = PlatformUI.getWorkbench().getDisplay();
		final SwtCallable<Boolean, RuntimeException> callable = () -> {
			return MessageDialog.openConfirm(display.getActiveShell(), "Download Flix version", String.format("Required Flix version '%s' is missing. Do you want to download it from '%s'", version.getKey(), url.toExternalForm()));
		};
		if (!(isUiThread ? callable.call() : display.syncCall(callable))) {
			throw new RuntimeException(String.format("Flix version '%s' is missing", version.getKey()));
		}
	}

	@Deprecated
	public synchronized static File loadFlixLibraryFolder(FlixVersion version, IProgressMonitor monitor) {
		return new File(loadFlixFolder(version, monitor), "src/library");
	}

	public synchronized static URI loadFlixJarUri(FlixVersion version, IProgressMonitor monitor) {
		return URI.create("jar:" + loadFlixJarFile(version, monitor).toURI().toString());
	}

	@Deprecated
	public synchronized static File loadFlixFolder(FlixVersion version, IProgressMonitor monitor) {
		final File flixSourceFolder = new File("_flix/flix." + version.getKey());
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
						file.setReadOnly();
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
