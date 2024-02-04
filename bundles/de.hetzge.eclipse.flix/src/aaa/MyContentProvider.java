package aaa;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.utils.JarUtils;
import de.hetzge.eclipse.utils.JarUtils.FolderJarItem;
import de.hetzge.eclipse.utils.JarUtils.JarItem;

public class MyContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof FolderJarItem) {
			final FolderJarItem folderJarItem = (FolderJarItem) parentElement;
			return getFolderChildren(folderJarItem);
		} else if (parentElement instanceof IProject) {
			final IProject project = (IProject) parentElement;
			return getProjectChildren(project);
		} else {
			return null;
		}
	}

	private Object[] getFolderChildren(FolderJarItem folderJarItem) {
		return folderJarItem.getChildren().toArray();
	}

	private Object[] getProjectChildren(IProject project) {
		final Optional<FlixProject> projectOptional = Flix.get().getModel().getFlixProject(project);
		if (projectOptional.isEmpty()) {
			return null;
		}
		final FlixProject flixProject = projectOptional.get();
		final File jarFile = flixProject.getFlixCompilerJarFile();
		try {
			final FolderJarItem rootFolder = JarUtils.indexJarFile(jarFile, "src/library/");
			return new Object[] { rootFolder };
		} catch (final IOException exception) {
			FlixLogger.logError(exception);
			return null; // TODO
		}
	}

	@Override
	public Object getParent(Object element) {
		if (!(element instanceof JarItem)) {
			return null;
		}
		final JarItem jarItem = (JarItem) element;
		return jarItem.getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof IProject || (element instanceof FolderJarItem && !((FolderJarItem) element).getChildren().isEmpty());
	}

}
