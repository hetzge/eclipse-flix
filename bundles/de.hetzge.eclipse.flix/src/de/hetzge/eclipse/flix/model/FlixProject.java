package de.hetzge.eclipse.flix.model;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.core.model.FlixVersion;
import de.hetzge.eclipse.flix.manifest.FlixManifestToml;
import de.hetzge.eclipse.flix.project.FlixProjectNature;
import de.hetzge.eclipse.flix.project.FlixProjectPreferences;
import de.hetzge.eclipse.flix.utils.FlixUtils;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

public class FlixProject {

	public static final String LIBRARY_FOLDER_NAME = "zzzlibrary";

	private final IProject project;
	private final FlixProjectPreferences projectPreferences;

	public FlixProject(IProject project) {
		this.project = project;
		this.projectPreferences = new FlixProjectPreferences(project);
	}

	public IProject getProject() {
		return this.project;
	}

	public FlixVersion getFlixVersion() {
		if (getInProjectFolderFlixCompilerJarFile().isPresent()) {
			return FlixVersion.CUSTOM_VERSION;
		} else {
			return getFlixToml().map(FlixManifestToml::getFlixVersion).orElse(FlixVersion.DEFAULT_VERSION);
		}
	}

	public Optional<FlixManifestToml> getFlixToml() {
		try {
			return FlixManifestToml.load(this.getProject().getFile("flix.toml"));
		} catch (final IOException exception) {
			FlixLogger.logError(String.format("Failed to read flix.toml in project '%s'", getProject().getName()), exception);
			return Optional.empty();
		}
	}

	public boolean isActive() {
		return isActiveFlixProject(this.project);
	}

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

	public File getFlixCompilerFolder() {
		final IProgressMonitor monitor = new NullProgressMonitor();
		final Optional<File> jarFileOptional = getInProjectFolderFlixCompilerJarFile();
		if (jarFileOptional.isPresent()) {
			return FlixUtils.getFlixCompilerFolder(FlixVersion.DEFAULT_VERSION, jarFileOptional.get(), monitor);
		} else {
			return FlixUtils.getFlixCompilerFolder(getFlixVersion(), FlixUtils.loadFlixJarFile(getFlixVersion(), monitor), monitor);
		}
	}

	public void deleteFlixCompilerFolder(IProgressMonitor monitor) {
		// Custom versions needs full refresh because content could have been changed
		if (getFlixVersion() == FlixVersion.CUSTOM_VERSION) {
			Utils.deleteDirectory(getFlixCompilerFolder());
		}
		try {
			getStandardLibraryFolder().delete(true, monitor);
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	public File createOrGetFlixCompilerFolder(IProgressMonitor monitor) {
		final Optional<File> jarFileOptional = getInProjectFolderFlixCompilerJarFile();
		if (jarFileOptional.isPresent()) {
			return FlixUtils.createOrGetFlixCompilerFolder(FlixVersion.DEFAULT_VERSION, jarFileOptional.get(), monitor);
		} else {
			final FlixVersion flixVersion = getFlixVersion();
			return FlixUtils.createOrGetFlixCompilerFolder(flixVersion, FlixUtils.loadFlixJarFile(flixVersion, monitor), monitor);
		}
	}

	public List<IFile> getFlixSourceFiles() {
		if (getSourceFolder().exists()) {
			return EclipseUtils.collectFiles(getSourceFolder(), file -> file.getFileExtension() != null && (file.getFileExtension().equals("flix")));
		} else {
			return List.of();
		}
	}

	public List<IFile> getFlixJarLibraryFiles() {
		if (getLibraryFolder().exists()) {
			return EclipseUtils.collectFiles(getLibraryFolder(), file -> file.getFileExtension() != null && (file.getFileExtension().equals("jar")));
		} else {
			return List.of();
		}
	}

	public List<IFile> getFlixFpkgLibraryFiles() {
		if (getLibraryFolder().exists()) {
			return EclipseUtils.collectFiles(getLibraryFolder(), file -> file.getFileExtension() != null && (file.getFileExtension().equals("fpkg")));
		} else {
			return List.of();
		}
	}

	public boolean isFlixSourceFile(IFile file) {
		if (!getSourceFolder().exists()) {
			return false;
		}
		return file.getFileExtension() != null && file.getFileExtension().equals("flix") && getSourceFolder().getRawLocation().isPrefixOf(file.getRawLocation());
	}

	public boolean isFlixJarLibraryFile(IFile file) {
		if (!getLibraryFolder().exists()) {
			return false;
		}
		return file.getFileExtension() != null && file.getFileExtension().equals("jar") && getLibraryFolder().getRawLocation().isPrefixOf(file.getRawLocation());
	}

	public boolean isFlixFpkgLibraryFile(IFile file) {
		if (!getLibraryFolder().exists()) {
			return false;
		}
		return file.getFileExtension() != null && file.getFileExtension().equals("fpkg") && getLibraryFolder().getRawLocation().isPrefixOf(file.getRawLocation());
	}

	public IFolder getSourceFolder() {
		return this.project.getFolder("src");
	}

	public IFolder getLibraryCacheFolder() {
		return getLibraryFolder().getFolder("cache");
	}

	public IFolder getLibraryFolder() {
		return this.project.getFolder("lib");
	}

	public IFolder getBuildFolder() {
		return this.project.getFolder("build");
	}

	public void deleteTemporaryFolders(IProgressMonitor progressMonitor) throws CoreException {
		deleteLibraryCacheFolder(progressMonitor);
		deleteBuildFolder(progressMonitor);
	}

	public void deleteLibraryCacheFolder(IProgressMonitor progressMonitor) throws CoreException {
		final IFolder libraryCacheFolder = getLibraryCacheFolder();
		if (libraryCacheFolder.exists()) {
			libraryCacheFolder.delete(true, progressMonitor);
		}
	}

	public void deleteBuildFolder(IProgressMonitor progressMonitor) throws CoreException {
		final IFolder buildFolder = getBuildFolder();
		if (buildFolder.exists()) {
			buildFolder.delete(true, progressMonitor);
		}
	}

	public FlixProjectPreferences getProjectPreferences() {
		return this.projectPreferences;
	}

	public boolean isLanguageToolingStarted() {
		return Flix.get().getLanguageToolingManager().isStarted(this);
	}

	public Optional<String> getLastLibHash() {
		return Optional.ofNullable(getPreferences().get("LAST_LIB_HASH", null));
	}

	public void setLastLibHash(String lastLibHash) {
		try {
			final IEclipsePreferences preferences = getPreferences();
			preferences.put("LAST_LIB_HASH", lastLibHash);
			preferences.flush();
		} catch (final BackingStoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	public Optional<String> getLastDependencyHash() {
		return Optional.ofNullable(getPreferences().get("LAST_DEPENDENCY_HASH", null));
	}

	public void setLastDependencyHash(String lastDependencyHash) {
		try {
			final IEclipsePreferences preferences = getPreferences();
			preferences.put("LAST_DEPENDENCY_HASH", lastDependencyHash);
			preferences.flush();
		} catch (final BackingStoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	private IEclipsePreferences getPreferences() {
		return new ProjectScope(this.project).getNode(FlixConstants.PLUGIN_ID);
	}

	public String calculateLibHash() {
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			final List<String> names = Stream.concat(
					getFlixFpkgLibraryFiles().stream().map(IFile::getName).sorted(),
					getFlixJarLibraryFiles().stream().map(IFile::getName).sorted())
					.collect(Collectors.toList());
			for (final String name : names) {
				messageDigest.update(name.getBytes());
			}
			final BigInteger bigInt = new BigInteger(1, messageDigest.digest());
			return bigInt.toString(16);
		} catch (final NoSuchAlgorithmException exception) {
			throw new RuntimeException(exception);
		}
	}

	public void refreshProjectFolders(IProgressMonitor monitor) {
		try {
			this.getSourceFolder().refreshLocal(IProject.DEPTH_INFINITE, monitor);
			this.getLibraryFolder().refreshLocal(IProject.DEPTH_INFINITE, monitor);
		} catch (final CoreException exception) {
			throw new RuntimeException("Failed to refresh project", exception);
		}
	}

	public IFolder createOrGetStandardLibraryFolder(IProgressMonitor monitor) {
		final IFolder libraryFolder = getStandardLibraryFolder();
		if (!libraryFolder.exists()) {
			try {
				final File standardLibraryFolder = new File(createOrGetFlixCompilerFolder(monitor), "src/library"); //$NON-NLS-1$
				libraryFolder.create(true, false, monitor);
				libraryFolder.setDerived(true, monitor);
				libraryFolder.createLink(Path.fromOSString(standardLibraryFolder.getAbsolutePath()), IResource.REPLACE, monitor);
				libraryFolder.refreshLocal(IFile.DEPTH_INFINITE, monitor);
			} catch (final CoreException exception) {
				throw new RuntimeException("Failed to create standard library folder", exception);
			}
		}
		return libraryFolder;
	}

	private IFolder getStandardLibraryFolder() {
		return this.project.getFolder(LIBRARY_FOLDER_NAME); // $NON-NLS-1$
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.project);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final FlixProject other = (FlixProject) obj;
		return Objects.equals(this.project, other.project);
	}

	public static boolean isActiveFlixProject(IProject project) {
		return SafeRunner.run(() -> project.isOpen() && project.getDescription().hasNature(FlixProjectNature.ID));
	}
}
