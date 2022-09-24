package de.hetzge.eclipse.flix.explorer;

import org.eclipse.handly.ui.EditorOpener;
import org.eclipse.handly.ui.action.OpenAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class FlixStandardLibraryTreeActionProvider extends CommonActionProvider {

	private OpenAction openAction;
	private boolean contribute;

	@Override
	public void init(ICommonActionExtensionSite aConfig) {
		System.out.println("FlixStandardLibraryTreeActionProvider.init()");
		if (aConfig.getViewSite() instanceof ICommonViewerWorkbenchSite) {
			final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			final FlixStandardLibraryTreeEditorUtils editorUtility = new FlixStandardLibraryTreeEditorUtils();
			final EditorOpener editorOpener = new EditorOpener(activePage, editorUtility);
			this.openAction = new OpenAction(editorOpener);
			this.contribute = true;
		}
	}

	@Override
	public void fillContextMenu(IMenuManager aMenu) {
		System.out.println("FlixStandardLibraryTreeActionProvider.fillContextMenu()");
		if (!this.contribute || getContext().getSelection().isEmpty()) {
			return;
		}
		this.openAction.selectionChanged((IStructuredSelection) getContext().getSelection());
	}

	@Override
	public void fillActionBars(IActionBars theActionBars) {
		System.out.println("FlixStandardLibraryTreeActionProvider.fillActionBars()");
		if (!this.contribute || getContext().getSelection().isEmpty()) {
			return;
		}
		this.openAction.selectionChanged((IStructuredSelection) getContext().getSelection());
		theActionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, this.openAction);
	}
}
