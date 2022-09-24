package de.hetzge.eclipse.flix.explorer;

import java.io.File;

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
				final File file = standardLibraryFile.getFile();
				if (file.isDirectory()) {
					return null;
				}
				final IFileStore fileStore = EFS.getStore(file.toURI());
				return new FileStoreEditorInput(fileStore);
			} catch (final CoreException exception) {
				throw new RuntimeException(exception);
			}
		} else {
			return super.getEditorInput(element);
		}
	}
}