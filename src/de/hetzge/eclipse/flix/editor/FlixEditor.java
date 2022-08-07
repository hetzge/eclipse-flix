package de.hetzge.eclipse.flix.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import de.hetzge.eclipse.flix.Activator;
import de.hetzge.eclipse.flix.FlixSourceViewerConfiguration;

/**
 * Flix text editor.
 */
public class FlixEditor extends AbstractDecoratedTextEditor {

	@Override
	protected void initializeEditor() {
		setPreferenceStore(getPreferenceStores());
		setDocumentProvider(Activator.getDefault().getFlixDocumentProvider());
		setSourceViewerConfiguration(new FlixSourceViewerConfiguration(getPreferenceStores(), this));
		setEditorContextMenuId("#FlixEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#FlixRulerContext"); //$NON-NLS-1$
	}

	private ChainedPreferenceStore getPreferenceStores() {
		return new ChainedPreferenceStore(new IPreferenceStore[] { Activator.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
	}

	@Override
	public void close(boolean save) {
		super.close(save);
	}

	@Override
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "de.hetzge.eclipse.flix.editor.scope" }); //$NON-NLS-1$
	}
}
