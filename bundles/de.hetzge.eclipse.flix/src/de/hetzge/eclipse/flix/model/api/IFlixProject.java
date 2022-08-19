package de.hetzge.eclipse.flix.model.api;

import org.eclipse.core.resources.IProject;

public interface IFlixProject {

	IProject getProject();

	boolean isActive();

}
