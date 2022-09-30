package de.hetzge.eclipse.flix.model.api;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.handly.model.IElementExtension;
import org.eclipse.handly.model.IModel;

public interface IFlixModel extends IElementExtension, IModel {

	List<FlixVersion> getUsedFlixVersions();

	List<IFlixProject> getFlixProjects();

	Optional<IFlixProject> getFlixProject(IProject project);

	IFlixJar getFlixJar(FlixVersion version);

	List<IFlixJar> getActiveFlixJars();

	Optional<IFlixJarNode> getFlixJarNode(URI uri);

	default IFlixProject getFlixProjectOrThrowCoreException(IProject project) throws CoreException {
		return getFlixProject(project).orElseThrow(() -> new CoreException(Status.error("Not a valid flix project")));
	}
}
