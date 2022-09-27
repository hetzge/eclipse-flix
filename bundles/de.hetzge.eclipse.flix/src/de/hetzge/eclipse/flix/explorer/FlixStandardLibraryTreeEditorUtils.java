package de.hetzge.eclipse.flix.explorer;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.ui.EditorUtility;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;

public final class FlixStandardLibraryTreeEditorUtils extends EditorUtility {
	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof FlixStandardLibraryFile) {
			final FlixStandardLibraryFile standardLibraryFile = (FlixStandardLibraryFile) element;
			try {
				final Path path = standardLibraryFile.getPath();
				if (Files.isDirectory(path)) {
					return null;
				}
				final IFileStore fileStore = EFS.getStore(path.toUri());
				return new FileStoreEditorInput(fileStore);
			} catch (final CoreException exception) {
				throw new RuntimeException(exception);
			}
		} else {
			return super.getEditorInput(element);
		}
	}
}