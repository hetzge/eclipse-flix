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
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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

public class FlixProject {

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
			return getFlixToml().map(FlixManifestToml::getFlixVersion).orElse(FlixConstants.FLIX_DEFAULT_VERSION);
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

	public void createStandardLibraryLinks() {
//		try {
//			final String jarBaseUri = FlixUtils.loadFlixJarUri(this.getFlixVersion(), null).toString();
//			final FolderJarItem folder = JarUtils.indexJarFile(getFlixCompilerJarFile(), "src/library");
//			for (final JarItem item : folder.getChildren()) {
//				if (item instanceof FolderJarItem) {
//					final FolderJarItem folderJarItem = (FolderJarItem) item;
//
//				} else if (item instanceof FileJarItem) {
//					final FileJarItem fileJarItem = (FileJarItem) item;
//					if (fileJarItem.getName().endsWith(".flix")) {
////						getProject().getFile(fileJarItem.getPath()).delete(true, null);
//						getProject().getFile(fileJarItem.getPath()).createLink(URI.create(jarBaseUri + "!/src/library/" + fileJarItem.getPath()), IResource.VIRTUAL, null);
//					}
//				}
//			}
//		} catch (final IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (final CoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
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

	public File getFlixFolder() {
		final IFile flixJarInProjectFile = this.project.getFile("flix.jar");
		if (flixJarInProjectFile.exists()) {
			return FlixUtils.loadFlixFolder(FlixConstants.FLIX_DEFAULT_VERSION, null); // TODO unpack flix jar from project
		} else {
			return FlixUtils.loadFlixFolder(getFlixVersion(), null);
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
