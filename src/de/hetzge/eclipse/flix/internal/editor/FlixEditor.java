package de.hetzge.eclipse.flix.internal.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import de.hetzge.eclipse.flix.internal.Activator;
import de.hetzge.eclipse.flix.internal.FlixSourceViewerConfiguration;

/**
 * Flix text editor.
 */
public class FlixEditor extends AbstractDecoratedTextEditor {
	@Override
	protected void initializeEditor() {
		final IPreferenceStore preferenceStore = new ChainedPreferenceStore(new IPreferenceStore[] { Activator.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
		setPreferenceStore(preferenceStore);
		setDocumentProvider(Activator.getDefault().getDocumentProvider());
		setSourceViewerConfiguration(new FlixSourceViewerConfiguration(preferenceStore, this));
		setEditorContextMenuId("#FlixEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#FlixRulerContext"); //$NON-NLS-1$
	}

	@Override
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "de.hetzge.eclipse.flix.editor.scope" }); //$NON-NLS-1$
	}
}
