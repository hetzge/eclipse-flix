package de.hetzge.eclipse.flix.editor.outline;

import org.eclipse.handly.ui.quickoutline.OutlinePopup;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IEditorInput;

public class FlixOutlinePopup extends OutlinePopup {

	private final IEditorInput editorInput;

	public FlixOutlinePopup(IEditorInput editorInput) {
		this.editorInput = editorInput;
	}

	@Override
	protected ITreeContentProvider getContentProvider() {
		return new FlixContentOutlineProvider();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FlixOutlineLabelProvider();
	}

	@Override
	protected Object computeInput() {
		return this.editorInput;
	}

	@Override
	protected Object getCorrespondingElement(ISelection hostSelection) {
		return null;
	}

	@Override
	protected boolean revealInHost(Object outlineElement) {
		return false;
	}

}