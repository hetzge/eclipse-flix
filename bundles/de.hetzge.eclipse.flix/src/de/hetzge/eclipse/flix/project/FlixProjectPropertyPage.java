package de.hetzge.eclipse.flix.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	private IProject project;
	private FlixProject flixProject;
	private FlixProjectPropertyPageControl control;

	@Override
	protected Control createContents(Composite parent) {
		this.project = (IProject) getElement().getAdapter(IResource.class);
		this.flixProject = Flix.get().getModel().getFlixProject(this.project).orElseThrow(() -> new IllegalStateException("Not a flix project"));
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
		this.flixProject.getProjectPreferences().save();
		return true;
	}

	private class FlixProjectPropertyPageControl extends Composite {

		public FlixProjectPropertyPageControl(Composite parent) {
			super(parent, SWT.NONE);
			setLayout(new GridLayout(2, true));

			final Group informationsGroup = createGroup(this, "Informations", 2, 2, GridData.FILL_HORIZONTAL);
			final Label versionLabel = new Label(informationsGroup, SWT.NONE);
			versionLabel.setText("Flix version");
			GridDataFactory.swtDefaults().applyTo(versionLabel);

			final Text versionText = new Text(informationsGroup, SWT.READ_ONLY);
			versionText.setText(FlixProjectPropertyPage.this.flixProject.getFlixVersion().getKey());
			GridDataFactory.fillDefaults().grab(true, false).applyTo(versionText);
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
