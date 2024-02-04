package aaa;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

public class MyActionProvider extends CommonActionProvider {

	public MyActionProvider() {
		System.out.println("MyActionProvider.MyActionProvider()");
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		System.out.println("MyActionProvider.fillContextMenu()");
//		if (this.fInViewPart) {
//			this.fOpenViewGroup.fillContextMenu(menu);
//		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		System.out.println("MyActionProvider.fillActionBars()");
//		if (this.fInViewPart) {
//			this.fOpenViewGroup.fillActionBars(actionBars);
//		}
		actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, new Action() {
			@Override
			public void run() {
				System.out.println("MyActionProvider.fillActionBars(...).new Action() {...}.run()");
				super.run();
			}
		});
	}

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		System.out.println("MyActionProvider.init()");
	}

//	@Override
//	public void init(ICommonActionExtensionSite site) {
//		ICommonViewerWorkbenchSite workbenchSite = null;
//		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite) {
//			workbenchSite = (ICommonViewerWorkbenchSite) site.getViewSite();
//		}
//		if (workbenchSite != null) {
//			if (workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
//				final IViewPart viewPart = (IViewPart) workbenchSite.getPart();
//
//				this.fOpenViewGroup = new OpenViewActionGroup(viewPart, site.getStructuredViewer());
//				this.fOpenViewGroup.containsOpenPropertiesAction(false);
//				this.fOpenViewGroup.containsShowInMenu(false);
//				this.fInViewPart = true;
//			}
//		}
//	}

}
