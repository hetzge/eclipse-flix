package de.hetzge.eclipse.flix.launch;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.ExecuteCommandRegistrationOptions;
import org.lxtk.CommandHandler;

import com.google.gson.JsonPrimitive;

import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixRunMainCommandHandler implements CommandHandler {

	@Override
	public ExecuteCommandRegistrationOptions getRegistrationOptions() {
		return new ExecuteCommandRegistrationOptions();
	}

	@Override
	public CompletableFuture<Object> execute(ExecuteCommandParams params) {
		System.out.println("FlixRunMainCommandHandler.execute(" + params + ")");
		final IFile file = EclipseUtils.activeFile().orElseThrow(() -> new IllegalStateException("No active file"));
		final String entrypoint = params.getArguments().stream().findFirst().filter(JsonPrimitive.class::isInstance).map(JsonPrimitive.class::cast).map(JsonPrimitive::getAsString).orElseThrow(() -> new IllegalArgumentException("Illegal entrypoint parameter"));
		FlixLaunchUtils.launchProject(file, ILaunchManager.RUN_MODE, "de.hetzge.eclipse.flix.launchConfigurationType", "Run", entrypoint);
		return CompletableFuture.completedFuture(null);
	}

}
