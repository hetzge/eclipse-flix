package de.hetzge.eclipse.flix.project;

import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.FlixVersion;

public class FlixNewProjectVersionPage extends WizardPage {

	private Combo versionCombo;

	protected FlixNewProjectVersionPage() {
		super("Flix Version");
	}

	@Override
	public void createControl(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		initializeDialogUnits(parent);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		createFlixVersionGroup(composite);
		setPageComplete(true);
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	private final void createFlixVersionGroup(Composite parent) {
		// version group
		final Composite versionGroup = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		versionGroup.setLayout(layout);
		versionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// version label
		final Label versionLabel = new Label(versionGroup, SWT.NONE);
		versionLabel.setText("Flix Version");
		versionLabel.setFont(parent.getFont());

		// version input field
		final GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 250;
		this.versionCombo = new Combo(versionGroup, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		this.versionCombo.setEnabled(true);
		this.versionCombo.setItems(FlixVersion.VERSIONS.stream().map(FlixVersion::getKey).collect(Collectors.toList()).toArray(new String[0]));
		this.versionCombo.setText(FlixConstants.FLIX_DEFAULT_VERSION.getKey());
		this.versionCombo.setLayoutData(data);
		this.versionCombo.setFont(parent.getFont());
	}

	public FlixVersion getVersionValue() {
		return this.versionCombo != null ? FlixVersion.getVersionByName(this.versionCombo.getText()).orElseThrow() : FlixConstants.FLIX_DEFAULT_VERSION;
	}

}
