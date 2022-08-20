package de.hetzge.eclipse.flix.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixProjectNature implements IProjectNature {

	public static final String ID = "de.hetzge.eclipse.flix.nature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		System.out.println("FlixProjectNature.configure()");
		EclipseUtils.addBuilder(this.project, FlixConstants.FLIX_BUILDER_ID);
	}

	@Override
	public void deconfigure() throws CoreException {
		System.out.println("FlixProjectNature.deconfigure()");
		EclipseUtils.removeBuilder(this.project, FlixConstants.FLIX_BUILDER_ID);
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
