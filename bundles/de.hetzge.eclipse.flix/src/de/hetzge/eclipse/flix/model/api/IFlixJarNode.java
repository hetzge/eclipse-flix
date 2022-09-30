package de.hetzge.eclipse.flix.model.api;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.eclipse.handly.model.IElement;

public interface IFlixJarNode extends IElement {

	boolean isDirectory();

	IFlixJar getJar();

	IFlixJarNode getParent();

	List<IFlixJarNode> getChildrenNodes();

	URI getUri();

	InputStream getInputStream();

}
