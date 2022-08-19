package de.hetzge.eclipse.flix;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.ui.IEditorInput;
import org.lxtk.lx4e.model.ILanguageSourceFile;

import de.hetzge.eclipse.flix.model.impl.FlixSourceFile;
import de.hetzge.eclipse.utils.EclipseUtils;

/**
 * Flix-specific implementation of {@link IInputElementProvider}.
 */
public class FlixInputElementProvider implements IInputElementProvider {
	/**
	 * The sole instance of the {@link FlixInputElementProvider}.
	 */
	public static final FlixInputElementProvider INSTANCE = new FlixInputElementProvider();

	private FlixInputElementProvider() {
	}

	@Override
	public ILanguageSourceFile getElement(IEditorInput editorInput) {
		if (editorInput == null) {
			return null;
		}
		final URI uri = EclipseUtils.getFile(editorInput).map(IFile::getLocationURI).orElseGet(() -> editorInput.getAdapter(IFileStore.class).toURI());
		return new FlixSourceFile(uri);
	}
}
