package de.hetzge.eclipse.flix;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

public class FlixJarFileSystem extends FileSystem {

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public boolean canDelete() {
		return false;
	}

	@Override
	public int attributes() {
		return EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_IMMUTABLE;
	}

	@Override
	public IFileStore getStore(URI uri) {
		if (uri == null) {
			return null;
		}

		final String uriString = uri.toString();
		if (uri.getScheme().equals("jar") && uriString.endsWith(".flix")) {

			final Matcher matcher = Pattern.compile(".*file:(.*.jar).*!\\/(.*\\.flix)").matcher(uriString);
			if (!matcher.find()) {
				throw new RuntimeException("Invalid jar uri: " + uriString);
			}
			final String jarFileUriString = matcher.group(1);
			final String pathInJar = matcher.group(2);
			final String[] parts = uriString.split("\\/");
			final String filename = parts[parts.length - 1];

			return new FileStore() {

				@Override
				public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
					return new String[0];
				}

				@Override
				public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
					final FileInfo fileInfo = new FileInfo();
					fileInfo.setName(filename);
					fileInfo.setExists(true);
					fileInfo.setLastModified(1L);
					fileInfo.setDirectory(false);
					fileInfo.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
					fileInfo.setAttribute(EFS.ATTRIBUTE_IMMUTABLE, true);
					return fileInfo;
				}

				@Override
				public IFileStore getChild(String name) {
					return null;
				}

				@Override
				public String getName() {
					return filename;
				}

				@Override
				public IFileStore getParent() {
					return null;
				}

				@Override
				public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
					JarFile jarFile = null;
					InputStream inputStream = null;
					try {
						jarFile = new JarFile(new File(jarFileUriString));
						inputStream = jarFile.getInputStream(jarFile.getJarEntry(pathInJar));
						return new ClosingInputStream(inputStream, jarFile);
					} catch (final Exception exception) {
						exception.printStackTrace();
						try {
							if (inputStream != null) {
								inputStream.close();
							}
							if (jarFile != null) {
								jarFile.close();
							}
						} catch (final Exception closeException) {
							exception.addSuppressed(closeException);
						}
						throw new CoreException(Status.error("Failed to open jar input stream", exception));
					}
				}

				@Override
				public URI toURI() {
					return uri;
				}
			};

		} else {
			try {
				return EFS.getStore(uri);
			} catch (final CoreException exception) {
				throw new RuntimeException(exception);
			}
		}
	}

	private static class ClosingInputStream extends FilterInputStream {
		private final AutoCloseable closeable;

		protected ClosingInputStream(InputStream inputStream, AutoCloseable closeable) {
			super(inputStream);
			this.closeable = closeable;
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			} finally {
				try {
					this.closeable.close();
				} catch (final Exception exception) {
					throw new RuntimeException(exception);
				}
			}
		}
	}
}
