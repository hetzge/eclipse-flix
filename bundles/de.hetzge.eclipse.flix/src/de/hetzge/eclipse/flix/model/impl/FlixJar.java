package de.hetzge.eclipse.flix.model.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.IElementExtension;
import org.eclipse.handly.model.impl.support.Body;
import org.eclipse.handly.model.impl.support.Element;
import org.eclipse.handly.model.impl.support.IElementImplSupport;
import org.eclipse.handly.model.impl.support.IModelManager;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.model.api.IFlixJar;
import de.hetzge.eclipse.flix.model.api.IFlixJarNode;
import de.hetzge.eclipse.flix.model.api.IFlixSourceFile;
import de.hetzge.eclipse.flix.utils.FlixUtils;

public class FlixJar extends Element implements IFlixJar, IElementExtension {

	private final FlixVersion version;

	public FlixJar(FlixModel model, FlixVersion version) {
		super(model, version.getKey());
		this.version = version;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public IFlixJar getJar() {
		return this;
	}

	@Override
	public IFlixJarNode getParent() {
		return null;
	}

	@Override
	public URI getUri() {
		return URI.create(FlixUtils.loadFlixJarUri(this.version, null).toString() + "!/");
	}

	@Override
	public InputStream getInputStream() {
		try {
			return new FileInputStream(FlixUtils.loadFlixJarFile(this.version, null));
		} catch (final FileNotFoundException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public InputStream getInputStream(String pathInJar) {
		JarFile jarFile = null;
		try {
			final File file = FlixUtils.loadFlixJarFile(this.version, null);
			jarFile = new JarFile(file);
			final JarEntry jarEntry = jarFile.getJarEntry(pathInJar);
			return new JarFileInputStream(jarFile, jarEntry);
		} catch (final Exception exception) {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (final IOException jarFileException) {
					exception.addSuppressed(jarFileException);
				}
			}
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException {
		System.out.println("FlixJar.buildStructure_()");

		try (FileSystem fileSystem = FileSystems.newFileSystem(getUri(), Map.of("create", "true"))) {
			final Body body = new Body();
			body.setChildren(children(this).toArray(Elements.EMPTY_ARRAY));
			context.get(IElementImplSupport.NEW_ELEMENTS).put(this, body);
		} catch (final IOException exception) {
			throw new CoreException(Status.error("Failed to read flix jar file", exception));
		}
	}

	private List<FlixJarNode> children(IFlixJarNode parent) {
		try {
			final Path path = Path.of(parent.getUri());
			if (Files.isDirectory(path)) {
				return Files.list(path).map(childPath -> {
					final URI childUri = childPath.toUri();
					final FlixJarNode flixJarNode = new FlixJarNode(parent, childUri);
					final List<FlixJarNode> childrenNodes = children(flixJarNode);
					flixJarNode.setChildrenNodes(childrenNodes);
					return flixJarNode;
				}).collect(Collectors.toList());
			} else {
				return List.of();
			}
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public List<IFlixSourceFile> getSourceFiles() {
		try {
			return Arrays.asList(getChildren(FlixSourceFile.class));
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public boolean isOpenable_() {
		return true;
	}

	@Override
	public void validateExistence_(IContext context) throws CoreException {
	}

	@Override
	public IModelManager getModelManager_() {
		return Flix.get().getModelManager();
	}

	public FlixVersion getVersion() {
		return this.version;
	}

	@Override
	public List<IFlixJarNode> getChildrenNodes() {
		try {
			return Arrays.asList(getChildren(IFlixJarNode.class));
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static class JarFileInputStream extends FilterInputStream {

		private final JarFile jarFile;

		protected JarFileInputStream(JarFile jarFile, JarEntry jarEntry) throws IOException {
			super(jarFile.getInputStream(jarEntry));
			this.jarFile = jarFile;
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			} finally {
				this.jarFile.close();
			}
		}
	}
}
