package de.hetzge.eclipse.flix.editor;

import org.eclipse.handly.ui.quickoutline.OutlinePopup;
import org.eclipse.handly.ui.quickoutline.OutlinePopupHandler;

/**
 * A handler that opens the Flix outline popup.
 */
public class FlixOutlinePopupHandler extends OutlinePopupHandler {
	@Override
	protected OutlinePopup createOutlinePopup() {
		return new FlixOutlinePopup();
	}
}
