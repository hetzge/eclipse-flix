package de.hetzge.eclipse.flix.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.flix.launch.FlixLauncher;

public class FlixBuildHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FlixLauncher.launchBuild(FlixHandlerUtils.getFlixProject());
		return null;
	}
}
