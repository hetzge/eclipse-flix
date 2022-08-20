package de.hetzge.eclipse.flix.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixLaunchConfigTab extends AbstractLaunchConfigurationTab {

	private final Image image;
	private FlixLaunchConfigurationControl control;

	public FlixLaunchConfigTab() {
		this.image = EclipseUtils.createImage("assets/icons/icon.png");
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
		this.control = new FlixLaunchConfigurationControl(parent);
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
		return "Flix";
	}

	private void modified(ModifyEvent event) {
		updateLaunchConfigurationDialog();
	}

	private class FlixLaunchConfigurationControl extends Composite {

		private final Text entrypointText;

		public FlixLaunchConfigurationControl(Composite parent) {
			super(parent, SWT.NONE);
			setLayout(new GridLayout(2, true));
			final Group group = createGroup(this, "Options", 2, 2, GridData.FILL_HORIZONTAL);

			// Entrypoint
			{
				final Label entrypointLabel = new Label(group, SWT.NONE);
				entrypointLabel.setText("Entrypoint");
				GridDataFactory.swtDefaults().applyTo(entrypointLabel);

				this.entrypointText = new Text(group, SWT.NONE);
				this.entrypointText.addModifyListener(FlixLaunchConfigTab.this::modified);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(this.entrypointText);
			}
		}

		public void setConfiguration(FlixLaunchConfiguration configuration) {
			configuration.getEntrypoint().ifPresent(entrypoint -> {
				this.entrypointText.setText(entrypoint);
			});
		}

		public void updateConfiguration(EditableFlixLaunchConfiguration configuration) {
			configuration.setEntrypoint(this.entrypointText.getText());
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
