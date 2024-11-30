package de.hetzge.eclipse.flix.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.hetzge.eclipse.flix.Flix;

public class FlixReconnectLanguageToolingHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Flix.get().getLanguageToolingManager().reconnectProject(FlixHandlerUtils.getFlixProject());
		return null;
	}
}
