package de.hetzge.eclipse.flix.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixLanguageToolingStateDecorator implements ILabelDecorator {
	public static final String ID = "de.hetzge.eclipse.flix.decorator.languageTooling";

	@Override
	public String decorateText(String text, Object element) {
		if (element instanceof IProject) {
			final IProject project = (IProject) element;
			if (Flix.get().getModel().getOrCreateFlixProject(project).map(FlixProject::isLanguageToolingStarted).orElse(false)) {
				return text + " (LSP)";
			}
		}
		return text;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public Image decorateImage(Image image, Object element) {
		return null;
	}
}
