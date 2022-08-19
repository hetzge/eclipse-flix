package de.hetzge.eclipse.flix.client;

import java.util.Collection;
import java.util.function.Consumer;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentUri;
import org.lxtk.WorkspaceService;
import org.lxtk.client.Feature;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.ui.EclipseLanguageClient;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.model.api.IFlixProject;

class FlixEclipseLanguageClient extends EclipseLanguageClient<LanguageServer> {

	private final IFlixProject flixProject;

	public FlixEclipseLanguageClient(EclipseLog log, IFlixProject flixProject, Consumer<PublishDiagnosticsParams> diagnosticConsumer, Collection<Feature<? super LanguageServer>> features) {
		super(log, diagnosticConsumer, Flix.get().getChangeFactory(), features);
		this.flixProject = flixProject;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return Flix.get().getWorkspaceService();
	}

	@Override
	protected String getMessageTitle(MessageParams params) {
		return "Flix Language Server"; //$NON-NLS-1$
	}

	@SuppressWarnings("deprecation")
	@Override
	public void fillInitializeParams(InitializeParams params) {
		super.fillInitializeParams(params);
		params.setRootUri(DocumentUri.convert(this.flixProject.getProject().getLocationURI()));
	}
}