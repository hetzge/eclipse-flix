package de.hetzge.eclipse.flix;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixTreeContentLabelProvider implements ICommonLabelProvider, IStyledLabelProvider {

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public Image getImage(Object element) {

		// TODO sort to the end?!

		if (element instanceof IFolder) {
			final IFolder folder = (IFolder) element;
			if (Objects.equals(FlixProject.LIBRARY_FOLDER_NAME, folder.getName())) {
				return FlixActivator.getImage(FlixImageKey.FLIX_ICON);
			}
		}

		return null;
	}

	@Override
	public String getText(Object element) {
		return null;
	}

	@Override
	public StyledString getStyledText(Object element) {

		if (element instanceof IFolder) {
			final IFolder folder = (IFolder) element;
			if (Objects.equals(FlixProject.LIBRARY_FOLDER_NAME, folder.getName())) {

				final Optional<FlixProject> flixProjectOptional = Flix.get().getModel().getFlixProject(folder.getProject());
				if (flixProjectOptional.isPresent()) {
					final FlixProject flixProject = flixProjectOptional.get();

					return new StyledString("Flix library " + flixProject.getFlixVersion().getKey());
				}

			}
		}

		return null;
	}

	@Override
	public String getDescription(Object anElement) {
		return null;
	}

	@Override
	public void restoreState(IMemento aMemento) {
	}

	@Override
	public void saveState(IMemento aMemento) {
	}

}
