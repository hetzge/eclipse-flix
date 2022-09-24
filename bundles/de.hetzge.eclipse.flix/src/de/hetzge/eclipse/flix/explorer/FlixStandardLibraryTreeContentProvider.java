package de.hetzge.eclipse.flix.explorer;

import java.util.stream.Collectors;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;

import de.hetzge.eclipse.flix.Flix;

public class FlixStandardLibraryTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		System.out.println("FlixStandardLibraryTreeContentProvider.getElements(" + inputElement + ")");
		if (inputElement instanceof IWorkspaceRoot) {
			return Flix.get().getModel().getUsedFlixVersions().stream().map(FlixStandardLibraryRoot::new).collect(Collectors.toList()).toArray(new Object[0]);
		} else {
			return null;
		}
	}

	@Override
	public boolean hasChildren(Object element) {
		System.out.println("FlixStandardLibraryTreeContentProvider.hasChildren(" + element + ")");
		if (element instanceof FlixStandardLibraryRoot) {
			final FlixStandardLibraryRoot libraryRoot = (FlixStandardLibraryRoot) element;
			return libraryRoot.hasChildren();
		} else if (element instanceof FlixStandardLibraryFile) {
			final FlixStandardLibraryFile standardLibraryFile = (FlixStandardLibraryFile) element;
			return standardLibraryFile.hasChildren();
		} else {
			return false;
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		System.out.println("FlixStandardLibraryTreeContentProvider.getChildren(" + parentElement + ")");
		if (parentElement instanceof FlixStandardLibraryRoot) {
			final FlixStandardLibraryRoot libraryRoot = (FlixStandardLibraryRoot) parentElement;
			return libraryRoot.getChildren();
		} else if (parentElement instanceof FlixStandardLibraryFile) {
			final FlixStandardLibraryFile standardLibraryFile = (FlixStandardLibraryFile) parentElement;
			return standardLibraryFile.getChildren();
		} else {
			return null;
		}
	}

	@Override
	public Object getParent(Object element) {
		System.out.println("FlixStandardLibraryTreeContentProvider.getParent(" + element + ")");
		return null;
	}
}
