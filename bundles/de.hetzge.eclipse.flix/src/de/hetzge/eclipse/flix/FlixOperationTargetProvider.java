package de.hetzge.eclipse.flix;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;

import de.hetzge.eclipse.flix.editor.FlixEditor;

/**
 * Flix operation target provider.
 *
 * @see LanguageOperationTarget
 */
public final class FlixOperationTargetProvider {

	private FlixOperationTargetProvider() {
		// private utils class constructor
	}

	/**
	 * Returns a Flix-specific operation target for the given file.
	 *
	 * @param file may be <code>null</code>
	 * @return the operation target, or <code>null</code> if none
	 */
	public static LanguageOperationTarget getOperationTarget(IFile file) {
		if (file == null) {
			return null;
		}
		return new LanguageOperationTarget(file.getLocationURI(), FlixConstants.LANGUAGE_ID, Flix.get().getLanguageService());
	}

	/**
	 * Returns a Flix operation target for the given editor.
	 *
	 * @param editor may be <code>null</code>
	 * @return the operation target, or <code>null</code> if none
	 */
	public static LanguageOperationTarget getOperationTarget(IEditorPart editor) {
		if (editor == null) {
			return null;
		}
		if (!(editor instanceof FlixEditor)) {
			return null;
		}
		final FlixEditor flixEditor = (FlixEditor) editor;
		final URI uri = flixEditor.getUri();
		return new LanguageOperationTarget(uri, FlixConstants.LANGUAGE_ID, Flix.get().getLanguageService());
	}
}
