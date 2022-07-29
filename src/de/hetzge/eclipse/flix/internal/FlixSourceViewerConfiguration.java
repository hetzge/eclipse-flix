package de.hetzge.eclipse.flix.internal;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.ui.completion.CompletionProposalSorter;
import org.lxtk.lx4e.ui.completion.ContentAssistProcessor;

/**
 * Configuration for a source viewer which shows Proto content.
 */
public class FlixSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private final ITextEditor editor;

	public FlixSourceViewerConfiguration(IPreferenceStore preferenceStore, ITextEditor editor) {
		super(preferenceStore);
		this.editor = editor;
	}

	@Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer viewer)
    {
        return new TMPresentationReconciler();
    }

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if (this.editor == null || !this.editor.isEditable()) {
			return null;
		}

		final ContentAssistant assistant = new ContentAssistant(true);
		assistant.setContentAssistProcessor(new ContentAssistProcessor(this::getLanguageOperationTarget), IDocument.DEFAULT_CONTENT_TYPE);
		assistant.setSorter(new CompletionProposalSorter());
		assistant.setInformationControlCreator(parent -> new DefaultInformationControl(parent, true));
		assistant.enableColoredLabels(true);
		return assistant;
	}

	private LanguageOperationTarget getLanguageOperationTarget() {
		return FlixOperationTargetProvider.getOperationTarget(this.editor);
	}
}
