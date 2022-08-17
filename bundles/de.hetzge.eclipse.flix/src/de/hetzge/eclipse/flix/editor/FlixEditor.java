package de.hetzge.eclipse.flix.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixDocumentProvider;

public class FlixEditor extends AbstractDecoratedTextEditor {

	private IContentOutlinePage outlinePage;

	@Override
	protected void initializeEditor() {
		final FlixDocumentProvider documentProvider = Flix.get().getDocumentProvider();
		setPreferenceStore(getPreferenceStores());
		setDocumentProvider(documentProvider);
		setSourceViewerConfiguration(new FlixSourceViewerConfiguration(getPreferenceStores(), this, documentProvider));
		setEditorContextMenuId("#FlixEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#FlixRulerContext"); //$NON-NLS-1$
	}

	private ChainedPreferenceStore getPreferenceStores() {
		return new ChainedPreferenceStore(new IPreferenceStore[] { FlixActivator.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
	}

	@Override
	public void close(boolean save) {
		super.close(save);
	}

	@Override
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "de.hetzge.eclipse.flix.editor.scope" }); //$NON-NLS-1$
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IContentOutlinePage.class) {
			if (this.outlinePage == null) {
				this.outlinePage = new FlixOutlinePage(this);
			}
			return adapter.cast(this.outlinePage);
		}
		return super.getAdapter(adapter);
	}

	public void closeOutlinePage() {
		if (this.outlinePage != null) {
			this.outlinePage = null;
			resetHighlightRange();
		}
	}
}
