package de.hetzge.eclipse.flix.editor.outline;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.handly.ui.quickoutline.IOutlinePopupHost;
import org.eclipse.handly.ui.quickoutline.OutlinePopup;
import org.eclipse.handly.ui.quickoutline.OutlinePopupHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.editor.FlixEditor;

public class FlixOutlinePopupHandler extends OutlinePopupHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (!(editor instanceof FlixEditor)) {
			return null;
		}
		final FlixEditor flixEditor = (FlixEditor) editor;
		Flix.get().getOutlineManager().queryOutline(flixEditor.getUri())
				.thenAccept(outline -> {
					PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
						final IOutlinePopupHost host = getOutlinePopupHost(event);
						if (host == null) {
							return;
						}
						final FlixOutlinePopup flixOutlinePopup = new FlixOutlinePopup(flixEditor, outline);
						flixOutlinePopup.init(host, getInvokingKeyStroke(event));
						flixOutlinePopup.open();
					});
				});
		return null;
	}

	@Override
	protected OutlinePopup createOutlinePopup() {
		return null; // not used
	}
}
