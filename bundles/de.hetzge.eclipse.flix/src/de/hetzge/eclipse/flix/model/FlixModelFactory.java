package de.hetzge.eclipse.flix.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import de.hetzge.eclipse.flix.core.model.FlixVersion;
import de.hetzge.eclipse.flix.manifest.DefaultFlixManifestToml;
import de.hetzge.eclipse.flix.manifest.FlixManifestToml;

public final class FlixModelFactory {

	private FlixModelFactory() {
	}

	public static FlixModel createFlixModel() {
		final FlixModel flixModel = new FlixModel();
		for (final IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (FlixProject.isActiveFlixProject(project)) {
				flixModel.addFlixProject(createFlixProject(project));
			}
		}
		return flixModel;
	}

	public static FlixProject createFlixProject(IProject project) {
		if (!FlixProject.isActiveFlixProject(project)) {
			throw new IllegalArgumentException("Not a valid Flix project: " + project.getName());
		}
		return new FlixProject(project, FlixManifestToml.load(project).orElseGet(() -> DefaultFlixManifestToml.createDefaultManifestToml(project.getName(), FlixVersion.DEFAULT_VERSION.getKey())));
	}

}
