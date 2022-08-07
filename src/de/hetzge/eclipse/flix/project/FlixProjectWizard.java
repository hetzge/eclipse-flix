package de.hetzge.eclipse.flix.project;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import de.hetzge.eclipse.flix.FlixUtils;
import de.hetzge.eclipse.utils.Utils;

public class FlixProjectWizard extends Wizard implements INewWizard {

	private IWorkbench workbench;
	private IStructuredSelection selection;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
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
		final File flixJarFile = FlixUtils.loadFlixJarFile();
		final File newProjectFolder = new File(page.getLocationPath().toFile(), page.getProjectName());
		final IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(page.getProjectName());

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
					final CreateProjectOperation projectOperation = new CreateProjectOperation(description, "Create flix project");
					projectOperation.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				} catch (final ExecutionException exception) {
					throw new InvocationTargetException(exception);
				}

			});
		} catch (InvocationTargetException | InterruptedException exception) {
			throw new RuntimeException(exception);
		}

		return true;
	}

}
