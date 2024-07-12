package de.hetzge.eclipse.flix.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixImageKey;

public class FlixTestLaunchConfigTab extends AbstractLaunchConfigurationTab {

	private final Image image;
	private FlixTestLaunchConfigurationControl control;

	public FlixTestLaunchConfigTab() {
		this.image = FlixActivator.getImage(FlixImageKey.FLIX3_ICON);
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
		this.control = new FlixTestLaunchConfigurationControl(parent);
		setControl(this.control);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		this.control.setConfiguration(new FlixLaunchConfiguration(configuration));
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		this.control.updateConfiguration(new EditableFlixLaunchConfiguration(configuration));
	}

	@Override
	public String getName() {
		return "Flix Test";
	}

	private void modified(ModifyEvent event) {
		updateLaunchConfigurationDialog();
	}

	private class FlixTestLaunchConfigurationControl extends Composite {

		public FlixTestLaunchConfigurationControl(Composite parent) {
			super(parent, SWT.NONE);
			setLayout(new GridLayout(2, true));
			final Group group = createGroup(this, "Options", 2, 2, GridData.FILL_HORIZONTAL);
		}

		public void setConfiguration(FlixLaunchConfiguration configuration) {
		}

		public void updateConfiguration(EditableFlixLaunchConfiguration configuration) {
		}
	}

	private static Group createGroup(Composite parent, String text, int columns, int hspan, int fill) {
		final Group g = new Group(parent, SWT.NONE);
		g.setLayout(new GridLayout(columns, false));
		g.setText(text);
		g.setFont(parent.getFont());
		final GridData gd = new GridData(fill);
		gd.horizontalSpan = hspan;
		g.setLayoutData(gd);
		return g;
	}
}
