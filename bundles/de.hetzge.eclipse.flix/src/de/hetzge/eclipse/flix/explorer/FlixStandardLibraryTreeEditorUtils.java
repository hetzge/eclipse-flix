package de.hetzge.eclipse.flix.explorer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.ui.EditorUtility;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;

import de.hetzge.eclipse.flix.model.api.IFlixSourceFile;

public final class FlixStandardLibraryTreeEditorUtils extends EditorUtility {
	@Override
	public IEditorInput getEditorInput(Object element) {
		System.out.println("FlixStandardLibraryTreeEditorUtils.getEditorInput(" + element + ")");
		if (element instanceof IFlixSourceFile) {
			final IFlixSourceFile flixSourceFile = (IFlixSourceFile) element;
			try {
				final IFileStore fileStore = EFS.getStore(flixSourceFile.getLocationUri());
				return new FileStoreEditorInput(fileStore);
			} catch (final CoreException exception) {
				throw new RuntimeException(exception);
			}
		} else {
			return super.getEditorInput(element);
		}
	}
}