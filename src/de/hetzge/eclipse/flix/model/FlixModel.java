package de.hetzge.eclipse.flix.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

import de.hetzge.eclipse.flix.Activator;

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
		return Activator.getDefault().getModelManager();
	}

	@Override
	public boolean isOpenable_() {
		return true;
	}

	@Override
	public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException {
		System.out.println("FlixModel.buildStructure_()");

		final List<FlixProject> projects = Arrays.asList(this.workspace.getRoot().getProjects()).stream() //
				.map(project -> new FlixProject(this, project)) //
				.collect(Collectors.toList());

		final Body body = new Body();
		body.setChildren(projects.toArray(Elements.EMPTY_ARRAY));
		context.get(IElementImplSupport.NEW_ELEMENTS).put(this, body);
	}

	@Override
	public List<IFlixProject> getProjects() {
		try {
			return Arrays.asList(getChildren()).stream().map(IFlixProject.class::cast).collect(Collectors.toList());
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}
}