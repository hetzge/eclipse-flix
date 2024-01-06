package de.hetzge.eclipse.flix.model.impl;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.flix.project.FlixProjectNature;
import de.hetzge.eclipse.flix.project.FlixProjectPreferences;
import de.hetzge.eclipse.flix.utils.FlixUtils;
import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixProject implements IFlixProject {

	private final IProject project;
	private final FlixProjectPreferences projectPreferences;

	public FlixProject(IProject project) {
		this.project = project;
		this.projectPreferences = new FlixProjectPreferences(project);
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public FlixVersion getFlixVersion() {
		if (getInProjectFolderFlixCompilerJarFile().isPresent()) {
			return FlixVersion.CUSTOM;
		} else {
			return this.projectPreferences.getFlixVersion().orElse(FlixConstants.FLIX_DEFAULT_VERSION);
		}
	}

	@Override
	public boolean isActive() {
		return isActiveFlixProject(this.project);
	}

	@Override
	public File getFlixCompilerJarFile() {
		final Optional<File> inProjectFolderFlixCompilerJarFileOptional = getInProjectFolderFlixCompilerJarFile();
		if (inProjectFolderFlixCompilerJarFileOptional.isPresent()) {
			return inProjectFolderFlixCompilerJarFileOptional.get();
		} else {
			return FlixUtils.loadFlixJarFile(getFlixVersion(), null);
		}
	}

	private Optional<File> getInProjectFolderFlixCompilerJarFile() {
		final IFile flixJarInProjectFile = this.project.getFile("flix.jar");
		if (flixJarInProjectFile.exists()) {
			return Optional.of(flixJarInProjectFile.getRawLocation().toFile());
		} else {
			return Optional.empty();
		}
	}

	@Override
	public File getFlixFolder() {
		final IFile flixJarInProjectFile = this.project.getFile("flix.jar");
		if (flixJarInProjectFile.exists()) {
			return FlixUtils.loadFlixFolder(FlixConstants.FLIX_DEFAULT_VERSION, null); // TODO unpack flix jar from project
		} else {
			return FlixUtils.loadFlixFolder(getFlixVersion(), null);
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

	@Override
	public FlixProjectPreferences getProjectPreferences() {
		return this.projectPreferences;
	}
}
