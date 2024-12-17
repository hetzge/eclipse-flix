package de.hetzge.eclipse.flix;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import de.hetzge.eclipse.flix.compiler.FlixCompilerProject;

public class FlixTreeContentLabelProvider implements ICommonLabelProvider, IStyledLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (!FlixCompilerProject.isFlixCompilerProject(element)) {
			return null;
		}
		return FlixActivator.getImage(FlixImageKey.FLIX_LIBRARY_ICON);
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
