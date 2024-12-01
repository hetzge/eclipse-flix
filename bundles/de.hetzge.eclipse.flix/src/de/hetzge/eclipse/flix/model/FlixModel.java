package de.hetzge.eclipse.flix.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.lxtk.WorkspaceFolder;

import de.hetzge.eclipse.flix.core.model.FlixVersion;
import de.hetzge.eclipse.flix.manifest.DefaultFlixManifestToml;
import de.hetzge.eclipse.flix.manifest.FlixManifestToml;

public class FlixModel {

	private final IWorkspace workspace;
	private final Map<IProject, FlixProject> flixProjectByEclipseProject;

	public FlixModel(IWorkspace workspace) {
		this.workspace = workspace;
		this.flixProjectByEclipseProject = new ConcurrentHashMap<>();
	}

	public List<FlixVersion> getUsedFlixVersions() {
		return getOrCreateFlixProjects().stream()
				.map(FlixProject::getFlixVersion)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<FlixProject> getOrCreateFlixProjects() {
		return Arrays.asList(this.workspace.getRoot().getProjects()).stream()
				.flatMap(project -> getOrCreateFlixProject(project).stream())
				.collect(Collectors.toList());
	}

	public Optional<FlixProject> getOrCreateFlixProject(IProject project) {
		if (!FlixProject.isActiveFlixProject(project)) {
			this.flixProjectByEclipseProject.remove(project);
			return Optional.empty();
		}
		return Optional.of(this.flixProjectByEclipseProject.computeIfAbsent(project, ignore -> {
			return new FlixProject(project, FlixManifestToml.load(project).orElseGet(() -> DefaultFlixManifestToml.createDefaultManifestToml(project.getName(), FlixVersion.DEFAULT_VERSION.getKey())));
		}));
	}

	public FlixProject getFlixProjectOrThrowCoreException(IProject project) throws CoreException {
		return getOrCreateFlixProject(project).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));
	}

	public List<WorkspaceFolder> getWorkspaceFolders() {
		return this.getOrCreateFlixProjects().stream().map(flixProject -> new WorkspaceFolder(flixProject.getProject().getLocationURI(), flixProject.getProject().getName())).collect(Collectors.toList());
	}
}