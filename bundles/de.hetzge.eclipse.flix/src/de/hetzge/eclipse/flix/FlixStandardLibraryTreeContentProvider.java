package de.hetzge.eclipse.flix;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;

import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.utils.FlixUtils;

public class FlixStandardLibraryTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		System.out.println("ContentProvider.getElements(" + inputElement + ")");
		if (inputElement instanceof IWorkspaceRoot) {
			return Flix.get().getModel().getUsedFlixVersions().stream().map(FlixStandardLibraryRoot::new).collect(Collectors.toList()).toArray(new Object[0]);
		} else {
			return null;
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		System.out.println("ContentProvider.getChildren(" + parentElement + ")");
		if (parentElement instanceof FlixStandardLibraryRoot) {
			final FlixStandardLibraryRoot libraryRoot = (FlixStandardLibraryRoot) parentElement;
			final File flixFolder = FlixUtils.loadFlixFolder(libraryRoot.getVersion(), null);
			if (!flixFolder.exists() || !flixFolder.isDirectory()) {
				return null;
			}
			final File libraryFolder = new File(flixFolder, "src/library");
			if (!libraryFolder.exists() || !libraryFolder.isDirectory()) {
				return null;
			}
			return libraryFolder.listFiles();
		} else if (parentElement instanceof File) {
			final File parentFile = (File) parentElement;
			if (!parentFile.isDirectory()) {
				return null;
			}
			return Arrays.asList(parentFile.listFiles()).toArray(new File[0]);
		} else {
			return null;
		}
	}

	@Override
	public Object getParent(Object element) {
		System.out.println("ContentProvider.getParent(" + element + ")");
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		System.out.println("ContentProvider.hasChildren(" + element + ")");
		if (element instanceof FlixStandardLibraryRoot) {
			return true;
		} else if (element instanceof File) {
			final File file = (File) element;
			if (file.isDirectory()) {
				final File[] files = file.listFiles();
				return files != null && files.length > 0;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static final class FlixStandardLibraryRoot {
		private final FlixVersion version;

		public FlixStandardLibraryRoot(FlixVersion version) {
			this.version = version;
		}

		public FlixVersion getVersion() {
			return this.version;
		}

		public String getName() {
			return String.format("Flix Standard Library %s", this.version.getKey());
		}
	}
}
