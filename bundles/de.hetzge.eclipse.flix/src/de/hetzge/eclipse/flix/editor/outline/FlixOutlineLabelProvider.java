package de.hetzge.eclipse.flix.editor.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixImageKey;

public class FlixOutlineLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (!(element instanceof DocumentSymbol)) {
			return element.toString();
		}
		final DocumentSymbol documentSymbol = (DocumentSymbol) element;
		return documentSymbol.getName();
	}

	@Override
	public Image getImage(Object element) {
		return FlixActivator.getImage(FlixImageKey.FLIX_ICON);
	}
}
