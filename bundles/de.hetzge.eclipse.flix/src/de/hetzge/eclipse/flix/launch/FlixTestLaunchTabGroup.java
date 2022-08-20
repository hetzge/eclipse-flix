package de.hetzge.eclipse.flix.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;

public class FlixTestLaunchTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		setTabs(new ILaunchConfigurationTab[] { new AbstractLaunchConfigurationTab() {

			@Override
			public void createControl(Composite parent) {
			}

			@Override
			public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
			}

			@Override
			public void initializeFrom(ILaunchConfiguration configuration) {
			}

			@Override
			public void performApply(ILaunchConfigurationWorkingCopy configuration) {
			}

			@Override
			public String getName() {
				return "Test Flix";
			}
		}, new CommonTab() });
	}
}
