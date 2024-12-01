package de.hetzge.eclipse.flix.launch;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.ExecuteCommandRegistrationOptions;
import org.lxtk.CommandHandler;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixRunReplCommandHandler implements CommandHandler {

	@Override
	public ExecuteCommandRegistrationOptions getRegistrationOptions() {
		return new ExecuteCommandRegistrationOptions();
	}

	@Override
	public CompletableFuture<Object> execute(ExecuteCommandParams params) {
		final FlixModel model = Flix.get().getModel();
		final FlixProject flixProject = EclipseUtils.activeFile().map(IFile::getProject).flatMap(model::getOrCreateFlixProject).orElseThrow(() -> new IllegalStateException("No active flix project found"));
		FlixLauncher.launchRepl(flixProject);
		return CompletableFuture.completedFuture(null);
	}
}
