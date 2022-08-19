package de.hetzge.eclipse.flix.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class FlixProjectNature implements IProjectNature {

	public static final String ID = "de.hetzge.eclipse.flix.nature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		System.out.println("FlixProjectNature.configure()");
	}

	@Override
	public void deconfigure() throws CoreException {
		System.out.println("FlixProjectNature.deconfigure()");
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
}
