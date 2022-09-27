package de.hetzge.eclipse.flix;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Collectors;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.internal.filesystem.NullFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

public class FileSystem2 extends FileSystem {

	@Override
	public IFileStore getStore(URI uri) {

		if (uri.getScheme().equals("jar")) {

			Flix.get().getJarFileSystemManager().enableFileSystem(uri);

			final Path path = Path.of(uri);
			return new FileStore() {

				@Override
				public URI toURI() {
					return uri;
				}

				@Override
				public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
					try {
						return Files.newInputStream(path);
					} catch (final IOException exception) {
						throw new CoreException(Status.error("Failed to open flix jar", exception));
					}
				}

				@Override
				public IFileStore getParent() {
					if (path.getParent() == null) {
						return null;
					}
					return getStore(Path.of(uri).getParent().toUri());
				}

				@Override
				public String getName() {
					System.out.println("FileSystem2.getStore(...).new FileStore() {...}.getName(" + uri + ")");
					final Path fileName = path.getFileName();
					if (fileName != null) {
						return fileName.toString();
					} else {
						return "";
					}
				}

				@Override
				public IFileStore getChild(String name) {
					return getStore(path.resolve(name).toUri());
				}

				@Override
				public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
					final FileInfo fileInfo = new FileInfo();
					fileInfo.setName(getName());
					fileInfo.setExists(true);
					fileInfo.setDirectory(Files.isDirectory(path));
					fileInfo.setLastModified(Instant.EPOCH.toEpochMilli() + 1L); // TODO
					try {
						fileInfo.setLength(Files.size(path));
					} catch (final IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return fileInfo;
				}

				@Override
				public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
					try {
						return Files.list(path).map(Path::getFileName).map(Path::toString).collect(Collectors.toList()).toArray(new String[0]);
					} catch (final IOException exception) {
						throw new CoreException(Status.error("Failed to get child names", exception));
					}
				}
			};
		} else {
			return NullFileSystem.getInstance().getStore(uri); // TODO
		}
	}

}
