package aaa;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.utils.JarUtils.FileJarItem;
import de.hetzge.eclipse.utils.JarUtils.JarItem;

public class MyLabelProvider implements ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof FileJarItem) {
			final FileJarItem fileJarItem = (FileJarItem) element;
			if (fileJarItem.getName().endsWith(".flix")) {
				return FlixActivator.getImage(FlixConstants.FLIX_ICON_IMAGE_KEY);
			} else {
				return FlixActivator.getImage(FlixConstants.FILE_ICON_IMAGE_KEY);
			}
		} else {
			return FlixActivator.getImage(FlixConstants.FOLDER_ICON_IMAGE_KEY);
		}
	}

	@Override
	public String getText(Object element) {
		if (element instanceof JarItem) {
			final JarItem jarItem = (JarItem) element;
			return jarItem.getName();
		}
		return element.toString();
	}

}
