package de.hetzge.eclipse.flix.project;

import java.io.File;
import java.io.IOException;
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
import de.hetzge.eclipse.flix.utils.FlixUtils;
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

		final FlixProjectPage page = (FlixProjectPage) getStartingPage();
		final File jreExecutableFile = Utils.getJreExecutable();
		final File flixJarFile = FlixUtils.loadFlixJarFile(FlixConstants.FLIX_DEFAULT_VERSION);
		final String projectName = page.getProjectName();
		final File newProjectFolder = new File(page.getLocationPath().toFile(), projectName);
		final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);

		try {
			getContainer().run(true, true, monitor -> {
				newProjectFolder.mkdirs();
				final ProcessBuilder processBuilder = new ProcessBuilder(jreExecutableFile.getAbsolutePath(), "-jar", flixJarFile.getAbsolutePath(), "init");
				processBuilder.directory(newProjectFolder);
				try {
					final Process process = processBuilder.start();
					process.waitFor();
				} catch (final IOException exception) {
					throw new RuntimeException(exception);
				}

				try {
					EclipseUtils.addNature(description, FlixProjectNature.ID);
					EclipseUtils.addBuilder(description, FlixConstants.FLIX_BUILDER_ID);
					final CreateProjectOperation projectOperation = new CreateProjectOperation(description, "Create flix project");
					projectOperation.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				} catch (final ExecutionException | CoreException exception) {
					throw new InvocationTargetException(exception);
				}
			});
		} catch (InvocationTargetException | InterruptedException exception) {
			throw new RuntimeException(exception);
		}

		return true;
	}

}
