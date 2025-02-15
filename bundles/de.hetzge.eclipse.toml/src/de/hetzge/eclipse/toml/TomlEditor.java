package de.hetzge.eclipse.toml;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

public class TomlEditor extends AbstractDecoratedTextEditor {

	@Override
	protected void initializeEditor() {
		setPreferenceStore(getPreferenceStores());
		setSourceViewerConfiguration(new TomlSourceViewerConfiguration(this));
	}

	private ChainedPreferenceStore getPreferenceStores() {
		return new ChainedPreferenceStore(new IPreferenceStore[] { TomlActivator.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
	}

	@Override
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "de.hetzge.eclipse.toml.editor.scope" }); //$NON-NLS-1$
	}
}
