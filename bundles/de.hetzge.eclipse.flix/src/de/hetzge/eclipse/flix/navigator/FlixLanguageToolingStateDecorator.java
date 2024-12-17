package de.hetzge.eclipse.flix.navigator;

import java.util.Optional;

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
			final Optional<FlixProject> flixProjectOptional = Flix.get().getModel().getOrCreateFlixProject(project);
			if (flixProjectOptional.isPresent()) {
				final FlixProject flixProject = flixProjectOptional.get();
				if (flixProjectOptional.get().isLanguageToolingStarted()) {
					return text + " (Flix " + flixProject.getFlixVersion().getKey() + ")";
				}
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
