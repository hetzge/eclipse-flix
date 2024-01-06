package de.hetzge.eclipse.flix.model.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;

import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.model.api.IFlixModel;
import de.hetzge.eclipse.flix.model.api.IFlixProject;

public class FlixModel implements IFlixModel {

	private final IWorkspace workspace;

	public FlixModel(IWorkspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public List<FlixVersion> getUsedFlixVersions() {
		return getFlixProjects().stream()
				.map(IFlixProject::getFlixVersion)
				.distinct()
				.collect(Collectors.toList());
	}

	@Override
	public List<IFlixProject> getFlixProjects() {
		return Arrays.asList(this.workspace.getRoot().getProjects()).stream()
				.map(FlixProject::new)
				.filter(IFlixProject::isActive)
				.collect(Collectors.toList());
	}

	@Override
	public Optional<IFlixProject> getFlixProject(IProject project) {
		return Optional.of(new FlixProject(project)).filter(IFlixProject::isActive).map(IFlixProject.class::cast);
	}
}