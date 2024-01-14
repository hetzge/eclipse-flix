package de.hetzge.eclipse.flix.editor.outline;

import org.eclipse.handly.ui.quickoutline.FilteringOutlinePopup;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.lsp4j.DocumentSymbol;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

import de.hetzge.eclipse.flix.editor.FlixEditor;
import de.hetzge.eclipse.flix.editor.outline.FlixOutlineManager.Outline;

public final class FlixOutlinePopup extends FilteringOutlinePopup {

	private final FlixEditor flixEditor;
	private final FlixOutlineContentProvider flixContentOutlineProvider;

	public FlixOutlinePopup(FlixEditor flixEditor, Outline outline) {
		this.flixEditor = flixEditor;
		this.flixContentOutlineProvider = new FlixOutlineContentProvider();
		this.flixContentOutlineProvider.setRootSymbols(outline.getRootSymbols());
	}

	@Override
	protected ITreeContentProvider getContentProvider() {
		return this.flixContentOutlineProvider;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FlixOutlineLabelProvider();
	}

	@Override
	protected boolean shouldUseHashlookup() {
		return true;
	}

	@Override
	protected Object computeInput() {
		return this.flixEditor.getEditorInput();
	}

	@Override
	protected Object getCorrespondingElement(ISelection hostSelection) {
		System.out.println("FlixOutlinePopup.getCorrespondingElement() " + hostSelection);
		return null; // TODO
	}

	@Override
	protected boolean revealInHost(Object outlineElement) {
		if (!(outlineElement instanceof DocumentSymbol)) {
			return false;
		}
		final DocumentSymbol documentSymbol = (DocumentSymbol) outlineElement;
		final IDocument document = DefaultEditorHelper.INSTANCE.getDocument(this.flixEditor);
		if (document == null) {
			return false;
		}
		try {
			final int startOffset = DocumentUtil.toOffset(document, documentSymbol.getSelectionRange().getStart());
			final int endOffset = DocumentUtil.toOffset(document, documentSymbol.getSelectionRange().getEnd());
			this.flixEditor.selectAndReveal(startOffset, endOffset - startOffset);
			return true;
		} catch (final BadLocationException exception) {
			throw new RuntimeException(exception);
		}
	}
}