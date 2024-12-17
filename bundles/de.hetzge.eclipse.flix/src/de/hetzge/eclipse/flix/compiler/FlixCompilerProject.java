package de.hetzge.eclipse.flix.compiler;

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
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SwtCallable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateProjectOperation;

import de.hetzge.eclipse.flix.core.model.FlixVersion;
import de.hetzge.eclipse.utils.Utils;

/**
 * The Flix Compiler Project is a special project in the workspace (managed by
 * the IDE itself) that contains the used Flix compilers (as jar and unpacked as
 * needed).
 */
public final class FlixCompilerProject {

	private static final ILog LOG = Platform.getLog(FlixCompilerProject.class);

	public static final String FLIX_COMPILER_PROJECT_NAME = "Flix";

	private final IProject project;

	private FlixCompilerProject(IProject project) {
		this.project = project;
	}

	public IResource getLibraryFolder(FlixVersion flixVersion) {
		return createOrGetFlixCompilerFolder(flixVersion, new NullProgressMonitor()).getFolder("src/library");
	}

	public synchronized File loadFlixJarFile(FlixVersion version, IProgressMonitor monitor) {
		final String flixJarName = "flix.v" + version.getKey() + ".jar";
		final File flixJarFile = this.project.getFile(flixJarName).getRawLocation().makeAbsolute().toFile();
		// delete file if it is corrupt
		if (flixJarFile.exists() && flixJarFile.length() == 0) {
			flixJarFile.delete();
		}
		if (!flixJarFile.exists()) {
			flixJarFile.getParentFile().mkdirs();
			LOG.info("Download " + flixJarName);
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
			createOrGetFlixCompilerFolder(version, monitor);
		}
		return flixJarFile;
	}

	public synchronized IFolder createOrGetFlixCompilerFolder(FlixVersion version, IProgressMonitor monitor) {
		final IFolder flixCompilerFolder = getFlixCompilerFolder(version, monitor);
		if (!flixCompilerFolder.exists()) {
			try {
				extract(loadFlixJarFile(version, monitor), flixCompilerFolder.getRawLocation().makeAbsolute().toFile(), monitor);
			} catch (final Exception exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixCompilerFolder;
	}

	private IFolder getFlixCompilerFolder(FlixVersion version, IProgressMonitor monitor) {
		return this.project.getFolder("flix." + version.getKey());
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

	private void extract(File archiveFile, File flixSourceFolder, IProgressMonitor monitor) throws Exception {
		try (JarArchiveInputStream inputStream = new JarArchiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile)))) {
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
					if (!entry.getName().startsWith("src/library")) {
						continue;
					}
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

	public static FlixCompilerProject createFlixCompilerProjectIfNotExists() {
		final IProject project = getFlixCompilerProject();
		if (project.exists()) {
			return new FlixCompilerProject(project);
		}
		return new FlixCompilerProject(createFlixCompilerProject());
	}

	private static IProject createFlixCompilerProject() {
		try {
			final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(FLIX_COMPILER_PROJECT_NAME);
			new CreateProjectOperation(description, "Create Flix Compiler Eclipse project").execute(new NullProgressMonitor(), null);
			final IProject project = getFlixCompilerProject();
			project.setDefaultCharset("UTF-8", new NullProgressMonitor()); //$NON-NLS-1$
			return project;
		} catch (final CoreException | ExecutionException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static IProject getFlixCompilerProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(FLIX_COMPILER_PROJECT_NAME);
	}

	public static boolean isFlixCompilerProject(Object element) {
		if (!(element instanceof IProject)) {
			return false;
		}
		final IProject project = (IProject) element;
		return Objects.equals(project.getName(), FlixCompilerProject.FLIX_COMPILER_PROJECT_NAME);
	}
}
