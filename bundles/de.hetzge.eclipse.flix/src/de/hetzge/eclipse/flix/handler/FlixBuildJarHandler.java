package de.hetzge.eclipse.flix.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.flix.launch.FlixLauncher;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixBuildJarHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FlixLauncher.launchBuildJar(FlixHandlerUtils.getFlixProject());
		return null;
	}
}
