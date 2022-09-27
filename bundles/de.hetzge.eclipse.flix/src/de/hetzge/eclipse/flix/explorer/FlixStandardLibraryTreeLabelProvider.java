package de.hetzge.eclipse.flix.explorer;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixConstants;

public class FlixStandardLibraryTreeLabelProvider implements ILabelProvider {

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
		if (element instanceof FlixStandardLibraryRoot) {
			return FlixActivator.getImage(FlixConstants.FLIX_ICON_IMAGE_KEY);
		} else if (element instanceof FlixStandardLibraryFile) {
			final FlixStandardLibraryFile standardLibraryFile = (FlixStandardLibraryFile) element;
			final Path path = standardLibraryFile.getPath();
			if (Files.isDirectory(path)) {
				return FlixActivator.getImage(FlixConstants.FOLDER_ICON_IMAGE_KEY);
			} else {
				return FlixActivator.getImage(FlixConstants.FLIX_ICON_IMAGE_KEY);
			}
		} else {
			return null;
		}
	}

	@Override
	public String getText(Object element) {
		if (element instanceof FlixStandardLibraryRoot) {
			final FlixStandardLibraryRoot libraryRoot = (FlixStandardLibraryRoot) element;
			return libraryRoot.getName();
		} else if (element instanceof FlixStandardLibraryFile) {
			final FlixStandardLibraryFile standardLibraryFile = (FlixStandardLibraryFile) element;
			return standardLibraryFile.getName();
		} else {
			return "???";
		}
	}

}
