package de.hetzge.eclipse.flix.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

public class FlixModel {

	private final IWorkspace workspace;

	public FlixModel(IWorkspace workspace) {
		this.workspace = workspace;
	}

	public List<FlixVersion> getUsedFlixVersions() {
		return getFlixProjects().stream()
				.map(FlixProject::getFlixVersion)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<FlixProject> getFlixProjects() {
		return Arrays.asList(this.workspace.getRoot().getProjects()).stream()
				.map(FlixProject::new)
				.filter(FlixProject::isActive)
				.collect(Collectors.toList());
	}

	public Optional<FlixProject> getFlixProject(IProject project) {
		return Optional.of(new FlixProject(project)).filter(FlixProject::isActive).map(FlixProject.class::cast);
	}

	public FlixProject getFlixProjectOrThrowCoreException(IProject project) throws CoreException {
		return getFlixProject(project).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));
	}
}