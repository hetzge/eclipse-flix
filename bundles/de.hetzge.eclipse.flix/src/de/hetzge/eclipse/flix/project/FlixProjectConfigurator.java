package de.hetzge.eclipse.flix.project;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import de.hetzge.eclipse.flix.manifest.FlixManifestToml;
import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixProjectConfigurator implements ProjectConfigurator {

	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		return new File(root, FlixManifestToml.FLIX_MANIFEST_TOML_FILE_NAME).exists() ? Set.of(root) : null;
	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return false;
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		return null;
	}

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredPaths, IProgressMonitor monitor) {
		return project.getFile(new Path(FlixManifestToml.FLIX_MANIFEST_TOML_FILE_NAME)).exists();
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredPaths, IProgressMonitor monitor) {
		try {
			EclipseUtils.addNature(project, FlixProjectNature.ID);
		} catch (final CoreException exception) {
			Display.getDefault().syncExec(() -> {
				ErrorDialog.openError(Display.getDefault().getActiveShell(), "Error", null, exception.getStatus());
			});
		}
	}
}
