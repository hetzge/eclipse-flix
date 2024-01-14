package de.hetzge.eclipse.flix.editor;

import java.time.Duration;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Throttler;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.lxtk.LanguageOperationTarget;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixDocumentProvider;
import de.hetzge.eclipse.flix.FlixOperationTargetProvider;
import de.hetzge.eclipse.flix.editor.outline.FlixOutlinePage;

public class FlixEditor extends AbstractDecoratedTextEditor {

	private final FlixOutlinePage outlinePage;
	private final Throttler syncOutlineThrottler;

	public FlixEditor() {
		this.syncOutlineThrottler = new Throttler(PlatformUI.getWorkbench().getDisplay(), Duration.ofMillis(250), this::syncOutline);
		this.outlinePage = new FlixOutlinePage(this);
	}

	@Override
	protected void initializeEditor() {
		final FlixDocumentProvider documentProvider = Flix.get().getDocumentProvider();
		setPreferenceStore(getPreferenceStores());
		setDocumentProvider(documentProvider);
		setSourceViewerConfiguration(new FlixSourceViewerConfiguration(getPreferenceStores(), this));
		setEditorContextMenuId("#FlixEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#FlixRulerContext"); //$NON-NLS-1$
	}

	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		this.syncOutlineThrottler.throttledExec();
	}

	private void syncOutline() {
		this.outlinePage.update(getSelectionProvider().getSelection());
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
	protected void editorSaved() {
		super.editorSaved();
		this.outlinePage.update();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IContentOutlinePage.class) {
			return adapter.cast(this.outlinePage);
		} else if (adapter == LanguageOperationTarget.class) {
			return adapter.cast(FlixOperationTargetProvider.getOperationTarget(this));
		} else {
			return super.getAdapter(adapter);
		}
	}
}
