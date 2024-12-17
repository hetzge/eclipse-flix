package de.hetzge.eclipse.flix.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.lxtk.WorkspaceFolder;

import de.hetzge.eclipse.flix.core.model.FlixVersion;

public class FlixModel {

	private final Map<IProject, FlixProject> flixProjectByEclipseProject;
	private final Map<FlixVersion, FlixCompiler> flixCompilerByFlixVersion;

	public FlixModel() {
		this.flixProjectByEclipseProject = new ConcurrentHashMap<>();
		this.flixCompilerByFlixVersion = new ConcurrentHashMap<>();
	}

	public void addFlixCompiler(FlixCompiler flixCompiler) {
		this.flixCompilerByFlixVersion.put(flixCompiler.getVersion(), flixCompiler);
	}

	public void addFlixProject(FlixProject flixProject) {
		this.flixProjectByEclipseProject.put(flixProject.getProject(), flixProject);
	}

	public Optional<FlixCompiler> getFlixCompilerForVersion(FlixVersion flixVersion) {
		return Optional.ofNullable(this.flixCompilerByFlixVersion.get(flixVersion));
	}

	public Optional<FlixProject> getFlixProject(IProject project) {
		return Optional.ofNullable(this.flixProjectByEclipseProject.get(project));
	}

	public FlixProject getFlixProjectOrThrowCoreException(IProject project) throws CoreException {
		return getFlixProject(project).orElseThrow(() -> new CoreException(Status.error(createNotValidFlixProjectMessage(project))));
	}

	public FlixProject getFlixProjectOrThrowExecutionException(IProject project) throws ExecutionException {
		return getFlixProject(project).orElseThrow(() -> new ExecutionException(createNotValidFlixProjectMessage(project)));
	}

	public FlixProject getFlixProjectOrThrowRuntimeException(IProject project) throws RuntimeException {
		return getFlixProject(project).orElseThrow(() -> new RuntimeException(createNotValidFlixProjectMessage(project)));
	}

	private String createNotValidFlixProjectMessage(IProject project) {
		return "Not a valid Flix project: " + project.getName(); //$NON-NLS-1$
	}

	public List<FlixVersion> getUsedFlixVersions() {
		return getFlixProjects().stream()
				.map(FlixProject::getFlixVersion)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<FlixProject> getFlixProjects() {
		return this.flixProjectByEclipseProject.values().stream()
				.filter(FlixProject::isActive)
				.collect(Collectors.toList());
	}

	public void removeProject(FlixProject flixProject) {
		this.flixProjectByEclipseProject.remove(flixProject.getProject());
	}

	public List<WorkspaceFolder> getWorkspaceFolders() {
		return this.getFlixProjects().stream().map(flixProject -> new WorkspaceFolder(flixProject.getProject().getLocationURI(), flixProject.getProject().getName())).collect(Collectors.toList());
	}

//	public List<FlixProject> getOrCreateFlixProjects() {
//	return Arrays.asList(this.workspace.getRoot().getProjects()).stream()
//			.flatMap(project -> getOrCreateFlixProject(project).stream())
//			.collect(Collectors.toList());
//}
//
//public Optional<FlixProject> getOrCreateFlixProject(IProject project) {
//	if (!FlixProject.isActiveFlixProject(project)) {
//		this.flixProjectByEclipseProject.remove(project);
//		return Optional.empty();
//	}
//	return Optional.of(this.flixProjectByEclipseProject.computeIfAbsent(project, ignore -> {
//		return new FlixProject(project, FlixManifestToml.load(project).orElseGet(() -> DefaultFlixManifestToml.createDefaultManifestToml(project.getName(), FlixVersion.DEFAULT_VERSION.getKey())));
//	}));
//}
}