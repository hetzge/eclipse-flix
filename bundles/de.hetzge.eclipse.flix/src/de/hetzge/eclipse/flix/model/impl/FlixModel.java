package de.hetzge.eclipse.flix.model.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.ApiLevel;
import org.eclipse.handly.context.Context;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.Elements;
import org.eclipse.handly.model.impl.IModelImpl;
import org.eclipse.handly.model.impl.support.Body;
import org.eclipse.handly.model.impl.support.Element;
import org.eclipse.handly.model.impl.support.IElementImplSupport;
import org.eclipse.handly.model.impl.support.IModelManager;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.api.FlixVersion;
import de.hetzge.eclipse.flix.model.api.IFlixModel;
import de.hetzge.eclipse.flix.model.api.IFlixProject;

public class FlixModel extends Element implements IFlixModel, IModelImpl {

	private final Context context;
	private final IWorkspace workspace;

	public FlixModel(Context context) {
		super(null, null);
		this.context = context;
		this.workspace = ResourcesPlugin.getWorkspace();
	}

	@Override
	public IContext getModelContext_() {
		return this.context;
	}

	@Override
	public int getModelApiLevel_() {
		return ApiLevel.CURRENT;
	}

	@Override
	public void validateExistence_(IContext context) throws CoreException {
		// always exists
	}

	@Override
	public IResource getResource_() {
		return this.workspace.getRoot();
	}

	@Override
	public IModelManager getModelManager_() {
		return Flix.get().getModelManager();
	}

	@Override
	public boolean isOpenable_() {
		return true;
	}

	@Override
	public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException {
		System.out.println("FlixModel.buildStructure_()");

		final List<IFlixProject> flixProjects = Arrays.asList(this.workspace.getRoot().getProjects()).stream() //
				.map(this::createFlixProject) //
				.filter(IFlixProject::isActive) //
				.collect(Collectors.toList());

		final Body body = new Body();
		body.setChildren(flixProjects.toArray(Elements.EMPTY_ARRAY));
		context.get(IElementImplSupport.NEW_ELEMENTS).put(this, body);
	}

	@Override
	public List<FlixVersion> getUsedFlixVersions() {
		return getFlixProjects().stream() //
				.map(flixProject -> flixProject.getFlixVersion()) //
				.distinct() //
				.collect(Collectors.toList());
	}

	@Override
	public List<IFlixProject> getFlixProjects() {
		try {
			return Arrays.asList(getChildren()).stream().map(IFlixProject.class::cast).collect(Collectors.toList());
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public Optional<IFlixProject> getFlixProject(IProject project) {
		final FlixProject referenceFlixProject = createFlixProject(project);
		return getFlixProjects().stream().filter(flixProject -> {
			return referenceFlixProject.equals(flixProject);
		}).findFirst();
	}

	private FlixProject createFlixProject(IProject project) {
		return new FlixProject(this, project);
	}
}