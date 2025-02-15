package de.hetzge.eclipse.flix.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixImageKey;
import de.hetzge.eclipse.flix.compiler.FlixCompilerProject;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixTreeContentLabelProvider implements ICommonLabelProvider, IStyledLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (FlixCompilerProject.isFlixCompilerProject(element)) {
			return FlixActivator.getImage(FlixImageKey.FLIX_LIBRARY_ICON);
		}
		if (element instanceof IFolder) {
			final IFolder folder = (IFolder) element;
			final IContainer parent = folder.getParent();
			if (Objects.equals(folder.getName(), "src") && parent instanceof IProject) {
				final IProject project = (IProject) parent;
				if (FlixProject.isActiveFlixProject(project)) {
					return FlixActivator.getImage(FlixImageKey.SRC_ICON);
				}
			} else if (Objects.equals(folder.getName(), "lib")) {
				return FlixActivator.getImage(FlixImageKey.LIB_ICON);
			} else if (Objects.equals(folder.getName(), "test")) {
				return FlixActivator.getImage(FlixImageKey.TEST_ICON);
			}
		}
		return null;
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (!FlixCompilerProject.isFlixCompilerProject(element)) {
			return null;
		}
		return new StyledString("Flix");
	}

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
	public String getText(Object element) {
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
