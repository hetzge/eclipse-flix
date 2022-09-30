package de.hetzge.eclipse.flix.project;

import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.model.api.IFlixProject;

public class FlixProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	private IProject project;
	private IFlixProject flixProject;
	private FlixProjectPropertyPageControl control;
	private FlixVersion previousFlixVersion;

	@Override
	protected Control createContents(Composite parent) {
		this.project = (IProject) getElement().getAdapter(IResource.class);
		this.flixProject = Flix.get().getModel().getFlixProject(this.project).orElseThrow(() -> new IllegalStateException("Not a flix project"));
		this.previousFlixVersion = this.flixProject.getFlixVersion();
		this.control = new FlixProjectPropertyPageControl(parent);
		return this.control;
	}

	@Override
	protected void performApply() {
		System.out.println("FlixProjectPropertyPage.performApply()");
		super.performApply();
	}

	@Override
	public boolean performOk() {
		System.out.println("FlixProjectPropertyPage.performOk()");
		final FlixVersion newFlixVersion = this.control.getFlixVersion();
		this.flixProject.getProjectPreferences().setFlixVersion(newFlixVersion);
		this.flixProject.getProjectPreferences().save();
		if (!this.previousFlixVersion.equals(newFlixVersion)) {
			Flix.get().getLanguageToolingManager().reconnectProject(this.flixProject);
			this.previousFlixVersion = newFlixVersion;
		}
		return true;
	}

	private class FlixProjectPropertyPageControl extends Composite {

		private final Combo entryPointCombo;

		public FlixProjectPropertyPageControl(Composite parent) {
			super(parent, SWT.NONE);
			setLayout(new GridLayout(2, true));
			final Group group = createGroup(this, "Options", 2, 2, GridData.FILL_HORIZONTAL);

			// Entrypoint
			{
				final Label entrypointLabel = new Label(group, SWT.NONE);
				entrypointLabel.setText("Flix version");
				GridDataFactory.swtDefaults().applyTo(entrypointLabel);

				final FlixVersion flixVersion = FlixProjectPropertyPage.this.flixProject.getFlixVersion();
				this.entryPointCombo = new Combo(group, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
				if (flixVersion.equals(FlixVersion.CUSTOM)) {
					this.entryPointCombo.setEnabled(false);
					this.entryPointCombo.setItems(flixVersion.getKey());
				} else {
					this.entryPointCombo.setEnabled(true);
					this.entryPointCombo.setItems(FlixVersion.VERSIONS.stream().map(FlixVersion::getKey).collect(Collectors.toList()).toArray(new String[0]));
				}
				this.entryPointCombo.setText(flixVersion.getKey());
				GridDataFactory.fillDefaults().grab(true, false).applyTo(this.entryPointCombo);
			}
		}

		public FlixVersion getFlixVersion() {
			return new FlixVersion(this.entryPointCombo.getText());
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
