package de.hetzge.eclipse.flix.navigator;

import java.util.Set;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider2;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

public class FlixTreeContentProvider implements IPipelinedTreeContentProvider2 {

	@Override
	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {
//		if (aParent instanceof IProject) {
//			for (final Object child : new ArrayList<>(theCurrentChildren)) {
//				if (child instanceof IFolder) {
//					System.out.println("xxx");
//					final IFolder folder = (IFolder) child;
//					if (Objects.equals(folder.getName(), FlixProject.LIBRARY_FOLDER_NAME)) {
//						System.out.println("aa");
//						theCurrentChildren.remove(child);
//						theCurrentChildren.add(child);
//					}
////					theCurrentChildren.add(child);
//				}
//			}
//		}

//		theCurrentChildren.add(new JarPackageFragmentRoot(null, Path.fromOSString("/home/hetzge/apps/eclipsepde202409/_flix/flix.v0.54.0.jar"), null, new IClasspathAttribute[0]));
	}

	@Override
	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
//		final HashSet children = new HashSet<>(theCurrentElements);
//		theCurrentElements.clear();
//
//		for (final Object children2 : children) {
//			theCurrentElements.add(children2);
//		}
	}

	@Override
	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		return aSuggestedParent;
	}

	@Override
	public PipelinedShapeModification interceptAdd(PipelinedShapeModification anAddModification) {
		return anAddModification;
	}

	@Override
	public PipelinedShapeModification interceptRemove(PipelinedShapeModification aRemoveModification) {
		return aRemoveModification;
	}

	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate aRefreshSynchronization) {
		return false;
	}

	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		return false;
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}

	@Override
	public void restoreState(IMemento aMemento) {
	}

	@Override
	public void saveState(IMemento aMemento) {
	}

	@Override
	public boolean hasPipelinedChildren(Object anInput, boolean currentHasChildren) {
		return currentHasChildren;
	}

}
