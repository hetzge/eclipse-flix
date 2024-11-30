package de.hetzge.eclipse.flix;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider2;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

public class FlixTreeContentProvider implements IPipelinedTreeContentProvider2 {

	@Override
	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {

		System.out.println("FlixTreeContentProvider.getPipelinedChildren() " + aParent.getClass().getName());
		if (aParent instanceof IProject) {
			for (final Object child : new ArrayList<>(theCurrentChildren)) {
				System.out.println(child.getClass().getName());
				if(child instanceof IFolder) {
					System.out.println("xxx");
					final IFolder folder = (IFolder) child;
//					theCurrentChildren.add(child);
				}
			}
		}

	}

	@Override
	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
		System.out.println("FlixTreeContentProvider.getPipelinedElements()");
//
//		final HashSet children = new HashSet<>(theCurrentElements);
//		theCurrentElements.clear();
//
//		for (final Object children2 : children) {
//			theCurrentElements.add(children2);
//		}
	}

	@Override
	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		System.out.println("FlixTreeContentProvider.getPipelinedParent()");
		return aSuggestedParent;
	}

	@Override
	public PipelinedShapeModification interceptAdd(PipelinedShapeModification anAddModification) {
		System.out.println("FlixTreeContentProvider.interceptAdd()");
		return anAddModification;
	}

	@Override
	public PipelinedShapeModification interceptRemove(PipelinedShapeModification aRemoveModification) {
		System.out.println("FlixTreeContentProvider.interceptRemove()");
		return aRemoveModification;
	}

	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate aRefreshSynchronization) {
		System.out.println("FlixTreeContentProvider.interceptRefresh()");
		return false;
	}

	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		System.out.println("FlixTreeContentProvider.interceptUpdate()");
		return false;
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
		System.out.println("FlixTreeContentProvider.init()");
	}

	@Override
	public Object[] getElements(Object inputElement) {
		System.out.println("FlixTreeContentProvider.getElements()");
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		System.out.println("FlixTreeContentProvider.getChildren()");
		return null;
	}

	@Override
	public Object getParent(Object element) {
		System.out.println("FlixTreeContentProvider.getParent()");
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		System.out.println("FlixTreeContentProvider.hasChildren()");
		return false;
	}

	@Override
	public void restoreState(IMemento aMemento) {
		System.out.println("FlixTreeContentProvider.restoreState()");
	}

	@Override
	public void saveState(IMemento aMemento) {
		System.out.println("FlixTreeContentProvider.saveState()");
	}

	@Override
	public boolean hasPipelinedChildren(Object anInput, boolean currentHasChildren) {
		System.out.println("FlixTreeContentProvider.hasPipelinedChildren()");
		return currentHasChildren;
	}

}
