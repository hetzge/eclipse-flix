package de.hetzge.eclipse.flix.model.impl;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import de.hetzge.eclipse.flix.model.api.IFlixJar;
import de.hetzge.eclipse.flix.model.api.IFlixJarNode;
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
		final List<FlixJar> flixJars = FlixVersion.VERSIONS.stream() //
				.map(version -> new FlixJar(this, version)) //
				.collect(Collectors.toList());

		final List<Object> bodyList = new ArrayList<>(flixProjects.size() + FlixVersion.VERSIONS.size());
		bodyList.addAll(flixProjects);
		bodyList.addAll(flixJars);
		final Body body = new Body();
		body.setChildren(bodyList.toArray(Elements.EMPTY_ARRAY));
		context.get(IElementImplSupport.NEW_ELEMENTS).put(this, body);
	}

	@Override
	public Optional<IFlixJarNode> getFlixJarNode(URI uri) {
		return getActiveFlixJars().stream().filter(jar -> Path.of(uri).startsWith(Path.of(jar.getUri()))).findFirst().flatMap(node -> {
			return visit(node, uri);
		});
	}

	private Optional<IFlixJarNode> visit(IFlixJarNode node, URI uri) {
		if (!uri.toString().startsWith(node.getUri().toString())) {
			return Optional.empty();
		} else if (node.getUri().equals(uri)) {
			return Optional.of(node);
		} else {
			final List<IFlixJarNode> childrenNodes = node.getChildrenNodes();
			for (final IFlixJarNode childNode : childrenNodes) {
				final Optional<IFlixJarNode> optionalNode = visit(childNode, uri);
				if (optionalNode.isPresent()) {
					return optionalNode;
				}
			}
			return Optional.empty();
		}
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
			return Arrays.asList(getChildren(IFlixProject.class)).stream().map(IFlixProject.class::cast).collect(Collectors.toList());
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

	@Override
	public IFlixJar getFlixJar(FlixVersion version) {
		try {
			return Stream.of(getChildren(FlixJar.class)).filter(jar -> jar.getVersion().equals(version)).findFirst().orElseThrow();
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public List<IFlixJar> getActiveFlixJars() {
		return getFlixProjects().stream().map(IFlixProject::getFlixVersion).map(this::getFlixJar).collect(Collectors.toList());
	}
}