package de.hetzge.eclipse.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.IAdaptable;

public final class JarUtils {
	private JarUtils() {
	}

	public static FolderJarItem indexJarFile(File file, String folder) throws IOException {
		if (!file.getName().endsWith(".jar")) { //$NON-NLS-1$
			throw new IllegalArgumentException("Not a jar file: " + file); //$NON-NLS-1$
		}
		try (JarFile jarFile = new JarFile(file)) {
			final Map<String, FolderJarItem> map = new HashMap<>();
			final Iterator<JarEntry> iterator = jarFile.entries().asIterator();
			final FolderJarItem rootFolder = new FolderJarItem("", null, new ArrayList<>());
			FolderJarItem resultFolder = rootFolder;
			while (iterator.hasNext()) {
				final JarEntry entry = iterator.next();
				final String path = entry.getName();
				final FolderJarItem parentFolder = map.getOrDefault(path.substring(0, (path.endsWith("/") ? Utils.secondLastIndex(path, "/") : path.lastIndexOf("/")) + 1), rootFolder);
				if (entry.isDirectory()) {
					final FolderJarItem folderJarItem = new FolderJarItem(path, parentFolder, new ArrayList<>());
					map.put(path, folderJarItem);
					parentFolder.getChildren().add(folderJarItem);
					if (folder != null && Objects.equals(folder, path)) {
						resultFolder = folderJarItem;
					}
				} else {
					parentFolder.getChildren().add(new FileJarItem(path, parentFolder));
				}
			}
			return resultFolder;
		}
	}

	public static abstract class JarItem {
		private final String path;
		private final String name;
		private final JarItem parent;

		public JarItem(String path, JarItem parent) {
			this.path = path;
			this.parent = parent;
			this.name = path.split("/")[path.split("/").length - 1];
		}

		public String getPath() {
			return this.path;
		}

		public String getName() {
			return this.name;
		}

		public JarItem getParent() {
			return this.parent;
		}
	}

	public static class FolderJarItem extends JarItem {
		private final List<JarItem> children;

		public FolderJarItem(String path, JarItem parent, List<JarItem> children) {
			super(path, parent);
			this.children = children;
		}

		public List<JarItem> getChildren() {
			return this.children;
		}
	}

	public static class FileJarItem extends JarItem implements IAdaptable {
		public FileJarItem(String path, JarItem parent) {
			super(path, parent);
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
//			if(Objects.equals(IResource.class, adapter)) {
//				return new IRes
//			}

//
//			  final IFile source = ...;//some source file
//			   final IFile link = source.getParent().getFile(new Path(source.getName() + ".link"));
//			   link.createLink(source.getLocationURI(), IResource.NONE, null);

			return null;
		}
	}
}
