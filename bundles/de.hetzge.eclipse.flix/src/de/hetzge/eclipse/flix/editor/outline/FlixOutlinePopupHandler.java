package de.hetzge.eclipse.flix.editor.outline;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.handly.ui.quickoutline.OutlinePopup;
import org.eclipse.handly.ui.quickoutline.OutlinePopupHandler;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class FlixOutlinePopupHandler extends OutlinePopupHandler {

	private IEditorInput editorInput;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		super.execute(event);
		final IEditorPart editor = HandlerUtil.getActiveEditor(event);
		this.editorInput = editor.getEditorInput();
		return null;
	}

	@Override
	protected OutlinePopup createOutlinePopup() {
		return new FlixOutlinePopup(this.editorInput);
	}
}
