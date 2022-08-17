package de.hetzge.eclipse.flix.project;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class FlixFilePage extends WizardNewFileCreationPage {

	public FlixFilePage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
	}
}
