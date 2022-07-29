package de.hetzge.eclipse.flix.internal;

import java.net.URI;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.lxtk.LanguageOperationTarget;

import de.hetzge.eclipse.flix.FlixCore;

/**
 * Flix operation target provider.
 *
 * @see LanguageOperationTarget
 */
public final class FlixOperationTargetProvider
{
    /**
     * Returns a Flix operation target for the given editor.
     *
     * @param editor may be <code>null</code>
     * @return the operation target, or <code>null</code> if none
     */
    public static LanguageOperationTarget getOperationTarget(IEditorPart editor)
    {
        if (editor == null) {
			return null;
		}

        URI documentUri = null;

        final IEditorInput editorInput = editor.getEditorInput();
        if (editorInput instanceof IURIEditorInput) {
			documentUri = ((IURIEditorInput)editorInput).getURI();
		}

        if (documentUri == null) {
			return null;
		}

        return new LanguageOperationTarget(documentUri, FlixCore.LANGUAGE_ID, FlixCore.LANGUAGE_SERVICE);
    }

    private FlixOperationTargetProvider()
    {
    	// private utils class constructor
    }
}
