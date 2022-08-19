package de.hetzge.eclipse.flix.model;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.handly.model.IElementExtension;

public interface IFlixModel extends IElementExtension {

	List<IFlixProject> getFlixProjects();

	Optional<IFlixProject> getFlixProject(IProject project);

}
