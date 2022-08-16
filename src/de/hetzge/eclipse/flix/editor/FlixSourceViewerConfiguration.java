package de.hetzge.eclipse.flix.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.handly.ui.IWorkingCopyManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.DocumentService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.ui.completion.CompletionProposalSorter;
import org.lxtk.lx4e.ui.completion.ContentAssistProcessor;
import org.lxtk.lx4e.ui.hover.DocumentHover;
import org.lxtk.lx4e.ui.hyperlinks.DeclarationHyperlinkDetector;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixOperationTargetProvider;

/**
 * Configuration for a source viewer which shows Flix content.
 */
public class FlixSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private final ITextEditor editor;
	private final IWorkingCopyManager workingCopyManager;

	public FlixSourceViewerConfiguration(IPreferenceStore preferenceStore, ITextEditor editor, IWorkingCopyManager workingCopyManager) {
		super(preferenceStore);
		this.editor = editor;
		this.workingCopyManager = workingCopyManager;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new DocumentHover(this::getLanguageOperationTarget);
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer viewer) {
		return new TMPresentationReconciler();
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		System.out.println("FlixSourceViewerConfiguration.getReconciler()");
		if (this.editor == null || !this.editor.isEditable() || this.workingCopyManager == null) {
			return null;
		}
		return new FlixReconciler(this.editor, this.workingCopyManager);
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		System.out.println("FlixSourceViewerConfiguration.getHyperlinkDetectors()");
		final IAdaptable context = new IAdaptable() {
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				if (adapter == LanguageOperationTarget.class) {
					return adapter.cast(getLanguageOperationTarget());
				} else if (adapter == DocumentService.class) {
					return adapter.cast(Flix.get().getDocumentService());
				} else {
					return null;
				}
			}
		};

		final DeclarationHyperlinkDetector declarationHyperlinkDetector = new DeclarationHyperlinkDetector();
		declarationHyperlinkDetector.setContext(context);
		return new IHyperlinkDetector[] { declarationHyperlinkDetector };
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
