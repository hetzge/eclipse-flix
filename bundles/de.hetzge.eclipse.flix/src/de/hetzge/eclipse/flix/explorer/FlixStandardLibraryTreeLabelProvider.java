package de.hetzge.eclipse.flix.explorer;

import org.eclipse.handly.ui.viewer.ElementLabelProvider;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.api.IFlixJar;
import de.hetzge.eclipse.flix.model.api.IFlixJarNode;

public class FlixStandardLibraryTreeLabelProvider extends ElementLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof IFlixJar) {
			return FlixActivator.getImage(FlixConstants.FOLDER_ICON_IMAGE_KEY);
		} else if (element instanceof IFlixJarNode) {
			return FlixActivator.getImage(FlixConstants.FLIX_ICON_IMAGE_KEY);
		} else {
			return null;
		}
	}
}
