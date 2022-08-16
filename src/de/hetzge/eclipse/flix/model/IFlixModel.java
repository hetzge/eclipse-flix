package de.hetzge.eclipse.flix.model;

import java.util.List;

import org.eclipse.handly.model.IElementExtension;

public interface IFlixModel extends IElementExtension {

	List<IFlixProject> getProjects();

}
