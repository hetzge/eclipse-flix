package de.hetzge.eclipse.flix.editor;

import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

public class FlixStructureSelectEnclosingAction extends Action {

	public static final String ACTION_DEFINITION_ID = "de.hetzge.eclipse.flix.editor.selectEnclosing";

	private final FlixEditor flixEditor;

	public FlixStructureSelectEnclosingAction(FlixEditor flixEditor) {
		this.flixEditor = flixEditor;
	}

	@Override
	public void run() {
		super.run();

		final ISelection selection = this.flixEditor.getSelectionProvider().getSelection();
		if (!(selection instanceof ITextSelection)) {
			return;
		}
		final ITextSelection textSelection = (ITextSelection) selection;
		final SourceRange sourceRange = new SourceRange(textSelection.getOffset(), textSelection.getLength());

		final String content = this.flixEditor.getDocumentProvider().getDocument(this.flixEditor.getEditorInput()).get();

		final int start = sourceRange.getOffset();
		final int end = sourceRange.getOffset() + sourceRange.getLength();
		int nextStart = -1;
		int nextEnd = -1;
		int openBraceCount = 0;
		for (int i = 1; i < 1000000000; i++) {
			if (nextStart == -1 && start - i >= 0) {
				if ("(,".contains(content.charAt(start - i) + "")) {
					nextStart = start - i;
				}
			} else {
				break;
			}
		}
		for (int i = 1; i < 1000000000; i++) {
			if (nextEnd == -1 && end + i < content.length()) {
				if('(' == content.charAt(end + i)) {
					openBraceCount++;
				}
				if(')' == content.charAt(end + i) && openBraceCount > 0) {
					openBraceCount--;
				}
				if (openBraceCount == 0 && "),".contains(content.charAt(end + i) + "")) {
					nextEnd = end + i;
				}
			} else {
				break;
			}
		}
		if (nextStart != -1 && nextEnd != -1) {
			this.flixEditor.selectAndReveal(nextStart + 1, nextEnd - (nextStart + 1));
		}

	}
}
