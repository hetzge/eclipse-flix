package de.hetzge.eclipse.flix;

import java.io.File;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.OpenSystemEditorAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class CommonActionProvider6 extends CommonActionProvider {

	private ICommonViewerWorkbenchSite viewSite;
	private OpenSystemEditorAction openFileAction;
	private boolean contribute;

	public CommonActionProvider6() {
		System.out.println("CommonActionProvider6.CommonActionProvider6()");
	}

	@Override
	public void init(ICommonActionExtensionSite aConfig) {
		if (aConfig.getViewSite() instanceof ICommonViewerWorkbenchSite) {
			this.viewSite = (ICommonViewerWorkbenchSite) aConfig.getViewSite();
			this.openFileAction = new OpenSystemEditorAction(this.viewSite.getPage()) {
				@Override
				public void run() {
					System.out.println("CommonActionProvider6.init(...).new OpenSystemEditorAction() {...}.run()");
					super.run();
				}
			};
			this.contribute = true;
		}
	}

	@Override
	public void fillContextMenu(IMenuManager aMenu) {
		System.out.println("CommonActionProvider6.fillContextMenu()");
		if (!this.contribute || getContext().getSelection().isEmpty()) {
			return;
		}
		final IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		this.openFileAction.selectionChanged(selection);
		if (this.openFileAction.isEnabled()) {
			aMenu.insertAfter(ICommonMenuConstants.GROUP_OPEN, this.openFileAction);
		}
	}

	@Override
	public void fillActionBars(IActionBars theActionBars) {
		System.out.println("CommonActionProvider6.fillActionBars()");
		if (!this.contribute) {
			return;
		}
		System.out.println(getContext().getInput().getClass());

		final IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		this.openFileAction.selectionChanged(selection);
		if (selection.size() == 1 && selection.getFirstElement() instanceof File) {
			theActionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, this.openFileAction);
		}
	}

}
