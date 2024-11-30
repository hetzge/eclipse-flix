package de.hetzge.eclipse.flix.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.flix.launch.FlixLauncher;

public class FlixOutdatedHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FlixLauncher.launchOutdated(FlixHandlerUtils.getFlixProject());
		return null;
	}
}
