package de.hetzge.eclipse.flix.model.api;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IFlixProject {

	IProject getProject();

	boolean isActive();

	File getFlixCompilerJarFile();

	List<IFile> getFlixSourceFiles();

	List<IFile> getFlixJarLibraryFiles();

	List<IFile> getFlixFpkgLibraryFiles();

	boolean isFlixSourceFile(IFile file);

	boolean isFlixJarLibraryFile(IFile file);

	boolean isFlixFpkgLibraryFile(IFile file);

	void restart();

	IFolder getSourceFolder();

	IFolder getLibraryFolder();

	IFolder getBuildFolder();

	void deleteBuildFolder(IProgressMonitor progressMonitor) throws CoreException;
}
