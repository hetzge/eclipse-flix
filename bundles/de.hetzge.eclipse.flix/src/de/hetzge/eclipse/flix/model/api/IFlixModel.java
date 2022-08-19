package de.hetzge.eclipse.flix.model.api;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.handly.model.IElementExtension;
import org.eclipse.handly.model.IModel;

public interface IFlixModel extends IElementExtension, IModel {

	List<IFlixProject> getFlixProjects();

	Optional<IFlixProject> getFlixProject(IProject project);

}
