package de.hetzge.eclipse.flix.launch;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixLaunchConfigTab extends AbstractLaunchConfigurationTab {

	private static final ILog LOG = Platform.getLog(FlixLaunchConfigTab.class);
	private final Image image;

	public FlixLaunchConfigTab() {
		this.image = EclipseUtils.createImage("/icon.png");
	}

	@Override
	public void dispose() {
		this.image.dispose();
		super.dispose();
	}

	@Override
	public Image getImage() {
		return this.image;
	}

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
		return "Flix";
	}
}
