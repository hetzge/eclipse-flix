package de.hetzge.eclipse.flix.model.impl;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.impl.support.Element;
import org.eclipse.handly.model.impl.support.IModelManager;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.flix.project.FlixProjectNature;
import de.hetzge.eclipse.flix.utils.FlixUtils;
import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixProject extends Element implements IFlixProject {

	private final IProject project;

	public FlixProject(FlixModel parent, IProject project) {
		super(parent, project.getName());
		this.project = project;
	}

	@Override
	public void validateExistence_(IContext context) throws CoreException {
		if (!isActive()) {
			throw newDoesNotExistException_();
		}
	}

	@Override
	public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public IModelManager getModelManager_() {
		return Flix.get().getModelManager();
	}

	@Override
	public IResource getResource_() {
		return this.project;
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public boolean isActive() {
		return isActiveFlixProject(this.project);
	}

	@Override
	public File getFlixCompilerJarFile() {
		final IFile flixJarInProjectFile = this.project.getFile("flix.jar");
		if (flixJarInProjectFile.exists()) {
			return flixJarInProjectFile.getRawLocation().toFile();
		} else {
			return FlixUtils.loadFlixJarFile(FlixConstants.FLIX_DEFAULT_VERSION);
		}
	}

	@Override
	public List<IFile> getFlixSourceFiles() {
		return EclipseUtils.collectFiles(getSourceFolder(), file -> file.getFileExtension() != null && (file.getFileExtension().equals("flix")));
	}

	@Override
	public List<IFile> getFlixJarLibraryFiles() {
		return EclipseUtils.collectFiles(getLibraryFolder(), file -> file.getFileExtension() != null && (file.getFileExtension().equals("jar")));
	}

	@Override
	public List<IFile> getFlixFpkgLibraryFiles() {
		return EclipseUtils.collectFiles(getLibraryFolder(), file -> file.getFileExtension() != null && (file.getFileExtension().equals("fpkg")));
	}

	@Override
	public boolean isFlixSourceFile(IFile file) {
		return file.getFileExtension() != null && file.getFileExtension().equals("flix") && getSourceFolder().getRawLocation().isPrefixOf(file.getRawLocation());
	}

	@Override
	public boolean isFlixJarLibraryFile(IFile file) {
		return file.getFileExtension() != null && file.getFileExtension().equals("jar") && getLibraryFolder().getRawLocation().isPrefixOf(file.getRawLocation());
	}

	@Override
	public boolean isFlixFpkgLibraryFile(IFile file) {
		return file.getFileExtension() != null && file.getFileExtension().equals("fpkg") && getLibraryFolder().getRawLocation().isPrefixOf(file.getRawLocation());
	}

	@Override
	public IFolder getSourceFolder() {
		return this.project.getFolder("src");
	}

	@Override
	public IFolder getLibraryFolder() {
		return this.project.getFolder("lib");
	}

	@Override
	public IFolder getBuildFolder() {
		return this.project.getFolder("build");
	}

	@Override
	public void deleteBuildFolder(IProgressMonitor progressMonitor) throws CoreException {
		final IFolder buildFolder = getBuildFolder();
		if (buildFolder.exists()) {
			buildFolder.delete(true, progressMonitor);
		}
	}

	public static boolean isActiveFlixProject(IProject project) {
		return SafeRunner.run(() -> project.isOpen() && project.getDescription().hasNature(FlixProjectNature.ID));
	}
}
