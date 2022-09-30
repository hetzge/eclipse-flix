package de.hetzge.eclipse.flix.model.api;

import java.io.InputStream;
import java.util.List;

public interface IFlixJar extends IFlixJarNode {

	List<IFlixSourceFile> getSourceFiles();

	InputStream getInputStream(String pathInJar);

}
