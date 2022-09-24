package de.hetzge.eclipse.flix.project;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.launch.FlixLauncher;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

public class FlixProjectWizard extends Wizard implements INewWizard {

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Flix Project Wizard");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		final FlixProjectPage page = new FlixProjectPage();
		page.setTitle("Flix Project");
		page.setDescription("Create a new Flix project");
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		System.out.println("FlixProjectWizard.performFinish()");

		try {
			final FlixProjectPage page = (FlixProjectPage) getStartingPage();
			final String projectName = page.getProjectName();
			final File newProjectFolder = new File(page.getLocationPath().toFile(), projectName);
			getContainer().run(true, true, monitor -> {
				try {
					final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);

					newProjectFolder.mkdirs();
					FlixLauncher.launchInit(newProjectFolder, FlixConstants.FLIX_DEFAULT_VERSION);

					EclipseUtils.addNature(description, FlixProjectNature.ID);
					EclipseUtils.addBuilder(description, FlixConstants.FLIX_BUILDER_ID);

					final CreateProjectOperation projectOperation = new CreateProjectOperation(description, "Create flix project");
					projectOperation.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				} catch (final ExecutionException | CoreException exception) {
					// Rollback already created files
					Utils.deleteFolder(newProjectFolder);
					throw new InvocationTargetException(exception);
				}
			});
		} catch (InvocationTargetException | InterruptedException exception) {
			throw new RuntimeException(exception);
		}

		return true;
	}

}
