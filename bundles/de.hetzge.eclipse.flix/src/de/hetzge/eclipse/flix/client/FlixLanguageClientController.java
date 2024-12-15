package de.hetzge.eclipse.flix.client;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.CommandService;
import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceService;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.BufferingDiagnosticConsumer;
import org.lxtk.client.CodeActionFeature;
import org.lxtk.client.CodeLensFeature;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.DeclarationFeature;
import org.lxtk.client.DocumentHighlightFeature;
import org.lxtk.client.DocumentSymbolFeature;
import org.lxtk.client.ExecuteCommandFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.FileOperationsFeature;
import org.lxtk.client.HoverFeature;
import org.lxtk.client.ReferencesFeature;
import org.lxtk.client.RenameFeature;
import org.lxtk.client.TextDocumentSyncFeature;
import org.lxtk.client.WorkspaceFoldersFeature;
import org.lxtk.client.WorkspaceSymbolFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.EclipseTextDocumentChangeEventMergeStrategy;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.ui.EclipseLanguageClientController;
import org.lxtk.util.Log;
import org.lxtk.util.SafeRun;
import org.lxtk.util.connect.SocketConnection;
import org.lxtk.util.connect.StreamBasedConnection;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.FlixMarkerResolutionGenerator;
import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixLanguageClientController extends EclipseLanguageClientController<LanguageServer> {

	private final int port;
	private final EclipseLog log;
	private final BufferingDiagnosticConsumer diagnosticConsumer;
	private final DocumentFilter documentFilter;
	private final FlixEclipseLanguageClient flixEclipseLanguageClient;

	FlixLanguageClientController(FlixProject project, int port) {
		this.port = port;
		this.log = new EclipseLog(FlixActivator.getDefault().getBundle(), "flix-language-client"); //$NON-NLS-1$
		this.diagnosticConsumer = new BufferingDiagnosticConsumer(new DiagnosticMarkers(FlixMarkerResolutionGenerator.MARKER_TYPE));
		this.documentFilter = new DocumentFilter(FlixConstants.LANGUAGE_ID, "file", project.getProject().getLocation().append("**").toString()); //$NON-NLS-1$ //$NON-NLS-2$
		final LanguageService languageService = Flix.get().getLanguageService();
		final DocumentService documentService = Flix.get().getDocumentService();
		final WorkspaceService workspaceService = Flix.get().getWorkspaceService();
		final CommandService commandService = Flix.get().getCommandService();
		final TextDocumentSyncFeature textDocumentSyncFeature = new TextDocumentSyncFeature(documentService);
		textDocumentSyncFeature.setChangeEventMergeStrategy(new EclipseTextDocumentChangeEventMergeStrategy());
		final List<Feature<? super LanguageServer>> features = new ArrayList<>();
		features.add(new CompletionFeature(languageService, commandService));
		features.add(FileOperationsFeature.newInstance(Flix.get().getPostResourceMonitor()));
		features.add(textDocumentSyncFeature);
		features.add(new ReferencesFeature(languageService));
		features.add(new DeclarationFeature(languageService));
		features.add(new HoverFeature(languageService));
		features.add(new DocumentSymbolFeature(languageService));
		features.add(new WorkspaceSymbolFeature(languageService, project));
		features.add(new WorkspaceFoldersFeature(workspaceService));
		features.add(new RenameFeature(languageService));
		features.add(new CodeLensFeature(languageService, commandService));
		features.add(new CodeActionFeature(languageService, commandService));
		features.add(new DocumentHighlightFeature(languageService));
		features.add(new ExecuteCommandFeature(commandService));
		this.flixEclipseLanguageClient = new FlixEclipseLanguageClient(this.log, project, this.diagnosticConsumer, features);
	}

	@Override
	protected AbstractLanguageClient<LanguageServer> getLanguageClient() {
		return this.flixEclipseLanguageClient;
	}

	public LanguageServer getLanguageServerApi() {
		return this.flixEclipseLanguageClient.getLanguageServerApi();
	}

	@Override
	protected JsonRpcConnectionFactory<LanguageServer> getConnectionFactory() {
		return new AbstractJsonRpcConnectionFactory<>() {
			@Override
			protected StreamBasedConnection newStreamBasedConnection() {
				try {
					return new SocketConnection(new Socket("localhost", FlixLanguageClientController.this.port)); //$NON-NLS-1$
				} catch (final IOException exception) {
					throw new RuntimeException(exception);
				}
			}
		};
	}

	@Override
	protected Log log() {
		return this.log;
	}

	@Override
	protected List<DocumentFilter> getDocumentSelector() {
		return Collections.singletonList(this.documentFilter);
	}

	@Override
	protected Class<LanguageServer> getServerInterface() {
		return LanguageServer.class;
	}

	@Override
	protected Duration getInitializeTimeout() {
		return Duration.ofSeconds(30);
	}

	@Override
	public void dispose() {
		try {
			this.diagnosticConsumer.dispose();
		} finally {
			super.dispose();
		}
	}

	public static FlixLanguageClientController connect(FlixProject project, int port) {
		return SafeRun.runWithResult(rollback -> {
			rollback.setLogger(FlixLogger::logError);
			final FlixLanguageClientController flixLanguageClientController = new FlixLanguageClientController(project, port);
			rollback.add(flixLanguageClientController::dispose);
			flixLanguageClientController.connect();
			return flixLanguageClientController;
		});
	}
}
