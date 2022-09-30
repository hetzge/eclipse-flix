package de.hetzge.eclipse.flix.explorer;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.handly.ui.viewer.ElementTreeContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import de.hetzge.eclipse.flix.Flix;

public class FlixStandardLibraryTreeContentProvider implements ITreeContentProvider {

	private final ElementTreeContentProvider contentProvider;

	public FlixStandardLibraryTreeContentProvider() {
		this.contentProvider = new ElementTreeContentProvider();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		System.out.println("FlixStandardLibraryTreeContentProvider.getElements(" + inputElement + ")");
		if (inputElement instanceof IWorkspaceRoot) {
			return Flix.get().getModel().getActiveFlixJars().toArray();
		} else {
			return null;
		}
	}

	@Override
	public boolean hasChildren(Object element) {
		return this.contentProvider.hasChildren(element);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return this.contentProvider.getChildren(parentElement);
	}

	@Override
	public Object getParent(Object element) {
		return this.contentProvider.getParent(element);
	}
}
