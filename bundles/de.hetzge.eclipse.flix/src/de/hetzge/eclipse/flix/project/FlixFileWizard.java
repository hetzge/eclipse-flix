package de.hetzge.eclipse.flix.project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.hetzge.eclipse.utils.StatusUtils;

public class FlixFileWizard extends Wizard implements INewWizard {

	private static final ILog LOG = Platform.getLog(FlixFileWizard.class);

	private FlixFilePage flixFilePage;
	private IStructuredSelection selection;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
		setWindowTitle("New Flix file");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		this.flixFilePage = new FlixFilePage(FlixFilePage.class.getSimpleName(), this.selection);
		this.flixFilePage.setTitle("New Flix file");
		this.flixFilePage.setDescription("Create a new .flix file");
		this.flixFilePage.setFileExtension("flix"); //$NON-NLS-1$
		addPage(this.flixFilePage);
	}

	@Override
	public boolean performFinish() {
		final String containerName = this.flixFilePage.getContainerFullPath().toOSString();
		final String fileName = this.flixFilePage.getFileName();
		try {
			getContainer().run(true, false, monitor -> {
				try {
					doFinish(containerName, fileName, monitor);
				} catch (final CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			});
		} catch (final InterruptedException exception) {
			return false;
		} catch (final InvocationTargetException exception) {
			StatusUtils.applyToStatusLine(this.flixFilePage, StatusUtils.createError(exception.getTargetException()));
			return false;
		}
		return true;
	}

	private void doFinish(String containerName, String fileName, IProgressMonitor monitor) throws CoreException {
		// create a dart file
		monitor.beginTask("Create new .flix file", 2);
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			StatusUtils.throwCoreException(String.format("Container %s does not exists.", containerName)); //$NON-NLS-1$
		}
		final IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		try (InputStream stream = new ByteArrayInputStream("".getBytes())) { //$NON-NLS-1$
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
		} catch (final IOException exception) {
			LOG.log(StatusUtils.createError(exception.getMessage()));
		}
		monitor.worked(1);
		monitor.setTaskName("Open new .flix file");
		getShell().getDisplay().asyncExec(() -> {
			final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			try {
				IDE.openEditor(page, file, true);
			} catch (final PartInitException exception) {
				LOG.log(StatusUtils.createError(exception.getMessage()));
			}
		});
		monitor.worked(1);
	}
}
