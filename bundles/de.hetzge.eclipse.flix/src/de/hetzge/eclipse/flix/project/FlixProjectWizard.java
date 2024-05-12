package de.hetzge.eclipse.flix.project;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.launch.FlixLauncher;
import de.hetzge.eclipse.flix.model.FlixVersion;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

public class FlixProjectWizard extends Wizard implements INewWizard {
	public static final String ID = "de.hetzge.eclipse.flix.newProject"; //$NON-NLS-1$

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Flix Project Wizard");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		final FlixNewProjectPage page = new FlixNewProjectPage();
		page.setTitle("Flix Project");
		page.setDescription("Create a new Flix project");
		addPage(page);

		final FlixNewProjectVersionPage versionPage = new FlixNewProjectVersionPage();
		versionPage.setTitle("Flix Version");
		versionPage.setDescription("Select Flix version");
		addPage(versionPage);
	}

	@Override
	public boolean performFinish() {
		try {
			final FlixNewProjectPage page = (FlixNewProjectPage) getStartingPage();
			final FlixNewProjectVersionPage versionPage = (FlixNewProjectVersionPage) page.getNextPage();
			final String projectName = page.getProjectName();
			final FlixVersion flixVersion = versionPage.getVersionValue();
			final File newProjectFolder = new File(page.getLocationPath().toFile(), projectName);
			getContainer().run(true, true, monitor -> {
				try {
					monitor.subTask("Init Flix project");
					newProjectFolder.mkdirs();
					final int code = FlixLauncher.launchInit(newProjectFolder, flixVersion).waitFor();
					if (code != 0) {
						throw new CoreException(Status.error("Init flix project process failed with code: " + code));
					}

					final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
					EclipseUtils.addNature(description, FlixProjectNature.ID);
					EclipseUtils.addBuilder(description, FlixConstants.FLIX_BUILDER_ID);

					final CreateProjectOperation projectOperation = new CreateProjectOperation(description, "Create Flix Eclipse project");
					projectOperation.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				} catch (ExecutionException | CoreException | InterruptedException exception) {
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
