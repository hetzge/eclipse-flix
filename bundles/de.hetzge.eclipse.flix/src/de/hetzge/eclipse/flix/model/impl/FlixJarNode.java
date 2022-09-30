package de.hetzge.eclipse.flix.model.impl;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.context.IContext;
import org.eclipse.handly.model.impl.support.Element;
import org.eclipse.handly.model.impl.support.IModelManager;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.api.IFlixJar;
import de.hetzge.eclipse.flix.model.api.IFlixJarNode;

public class FlixJarNode extends Element implements IFlixJarNode {

	private final IFlixJarNode parent;
	private final URI uri;
	private final List<IFlixJarNode> children;

	public FlixJarNode(IFlixJarNode parent, URI uri) {
		super(parent, uri.toString());
		this.parent = parent;
		this.uri = uri;
		this.children = new ArrayList<>();
	}

	@Override
	public IFlixJarNode getParent() {
		return this.parent;
	}

	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public void validateExistence_(IContext context) throws CoreException {
	}

	@Override
	public void buildStructure_(IContext context, IProgressMonitor monitor) throws CoreException {
	}

	@Override
	public boolean isOpenable_() {
		return false;
	}

	@Override
	public IModelManager getModelManager_() {
		return Flix.get().getModelManager();
	}

	@Override
	public boolean isDirectory() {
		return !this.uri.toString().endsWith(".flix");
	}

	@Override
	public IFlixJar getJar() {
		return getParent().getJar();
	}

	@Override
	public InputStream getInputStream() {
		return getJar().getInputStream(this.uri.toString().split("!\\/")[0]);
	}

	void setChildrenNodes(List<FlixJarNode> children) {
		this.children.clear();
		this.children.addAll(children);
	}

	@Override
	public List<IFlixJarNode> getChildrenNodes() {
		return this.children;
	}
}
