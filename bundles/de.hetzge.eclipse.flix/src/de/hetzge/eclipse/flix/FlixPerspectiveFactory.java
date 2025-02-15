package de.hetzge.eclipse.flix;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;

import de.hetzge.eclipse.flix.project.FlixFileWizard;
import de.hetzge.eclipse.flix.project.FlixProjectWizard;

public class FlixPerspectiveFactory implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		final String editorArea = layout.getEditorArea();

		final IFolderLayout folder = layout.createFolder("left", IPageLayout.LEFT, (float) 0.25, editorArea); //$NON-NLS-1$
		folder.addView(IPageLayout.ID_PROJECT_EXPLORER);

		final IFolderLayout outputfolder = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.75, editorArea); //$NON-NLS-1$
		outputfolder.addView(IPageLayout.ID_PROBLEM_VIEW);
		outputfolder.addView(NewSearchUI.SEARCH_VIEW_ID);
		outputfolder.addView(IConsoleConstants.ID_CONSOLE_VIEW);
		outputfolder.addView(IProgressConstants.PROGRESS_VIEW_ID);

		final IFolderLayout outlineFolder = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.75, editorArea); //$NON-NLS-1$
		outlineFolder.addView(IPageLayout.ID_OUTLINE);

		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

		// new actions
		layout.addNewWizardShortcut(FlixFileWizard.ID);
		layout.addNewWizardShortcut(FlixProjectWizard.ID);
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard"); //$NON-NLS-1$
	}
}
