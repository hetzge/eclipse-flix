package de.hetzge.eclipse.flix;

import java.io.InputStream;
import java.net.URI;
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

import de.hetzge.eclipse.flix.model.api.IFlixJarNode;

public class FileSystem2 extends FileSystem {

	@Override
	public IFileStore getStore(URI uri) {
		if (uri == null) {
			return null;
		}

		if (uri.getScheme().equals("jar")) {

			final IFlixJarNode flixJarNode = Flix.get().getModel().getFlixJarNode(uri).orElseThrow();

			return new FileStore() {

				@Override
				public URI toURI() {
					return uri;
				}

				@Override
				public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
					return flixJarNode.getInputStream();
				}

				@Override
				public IFileStore getParent() {
					return getStore(flixJarNode.getParent().getUri());
				}

				@Override
				public String getName() {
					return flixJarNode.getUri().toString();
				}

				@Override
				public IFileStore getChild(String name) {
					return getStore(flixJarNode.getChildrenNodes().stream().filter(node -> node.getUri().toString().equals(name)).map(node -> node.getUri()).findFirst().orElse(null));
				}

				@Override
				public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
					final FileInfo fileInfo = new FileInfo();
					fileInfo.setName(getName());
					fileInfo.setExists(true);
					fileInfo.setDirectory(flixJarNode.isDirectory());
					fileInfo.setLastModified(Instant.EPOCH.toEpochMilli() + 1L); // TODO
//					try {
//						fileInfo.setLength(Files.size(path));
//					} catch (final IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					return fileInfo;
				}

				@Override
				public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
					return flixJarNode.getChildrenNodes().stream().map(IFlixJarNode::getUri).map(URI::toString).collect(Collectors.toList()).toArray(new String[0]);
				}
			};
		} else {
			return NullFileSystem.getInstance().getStore(uri); // TODO
		}
	}

}
