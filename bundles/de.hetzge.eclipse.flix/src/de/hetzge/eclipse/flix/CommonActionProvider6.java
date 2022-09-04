package de.hetzge.eclipse.flix;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

public class CommonActionProvider6 extends CommonActionProvider {

	public CommonActionProvider6() {
		System.out.println("CommonActionProvider6.CommonActionProvider6()");
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		System.out.println("CommonActionProvider6.fillActionBars()");
		super.fillActionBars(actionBars);
	}

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		System.out.println("CommonActionProvider6.init()");
		super.init(aSite);
	}

}
