package de.hetzge.eclipse.flix.model.api;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

public interface IFlixModel {

	List<FlixVersion> getUsedFlixVersions();

	List<IFlixProject> getFlixProjects();

	Optional<IFlixProject> getFlixProject(IProject project);

	default IFlixProject getFlixProjectOrThrowCoreException(IProject project) throws CoreException {
		return getFlixProject(project).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));
	}
}
