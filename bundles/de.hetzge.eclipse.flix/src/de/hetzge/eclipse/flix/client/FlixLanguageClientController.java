package de.hetzge.eclipse.flix.client;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.BufferingDiagnosticConsumer;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.DeclarationFeature;
import org.lxtk.client.DocumentSymbolFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.FileOperationsFeature;
import org.lxtk.client.HoverFeature;
import org.lxtk.client.ReferencesFeature;
import org.lxtk.client.RenameFeature;
import org.lxtk.client.TextDocumentSyncFeature;
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
import de.hetzge.eclipse.flix.FlixMarkerResolutionGenerator;
import de.hetzge.eclipse.flix.model.api.IFlixProject;

public class FlixLanguageClientController extends EclipseLanguageClientController<LanguageServer> {

	private final IFlixProject flixProject;
	private final int port;
	private final EclipseLog log;
	private final BufferingDiagnosticConsumer diagnosticConsumer;
	private final DocumentFilter documentFilter;
	private final LanguageService languageService;
	private final DocumentService documentService;

	public FlixLanguageClientController(IFlixProject flixProject, int port) {
		this.flixProject = flixProject;
		this.port = port;
		this.log = new EclipseLog(FlixActivator.getDefault().getBundle(), "flix-language-client:" + flixProject.getProject().getName()); //$NON-NLS-1$
		this.diagnosticConsumer = new BufferingDiagnosticConsumer(new DiagnosticMarkers(FlixMarkerResolutionGenerator.MARKER_TYPE));
		this.documentFilter = new DocumentFilter(FlixConstants.LANGUAGE_ID, "file", this.flixProject.getProject().getLocation().append("**").toString()); //$NON-NLS-1$ //$NON-NLS-2$
		this.languageService = Flix.get().getLanguageService();
		this.documentService = Flix.get().getDocumentService();
	}

	@Override
	protected AbstractLanguageClient<LanguageServer> getLanguageClient() {
		return new FlixEclipseLanguageClient(this.log, this.flixProject, this.diagnosticConsumer, createFeatureList());
	}

	private List<Feature<? super LanguageServer>> createFeatureList() {
		final TextDocumentSyncFeature textDocumentSyncFeature = new TextDocumentSyncFeature(this.documentService);
		textDocumentSyncFeature.setChangeEventMergeStrategy(new EclipseTextDocumentChangeEventMergeStrategy());

		final List<Feature<? super LanguageServer>> features = new ArrayList<>();
		features.add(new CompletionFeature(this.languageService));
		features.add(FileOperationsFeature.newInstance(Flix.get().getResourceMonitor()));
		features.add(textDocumentSyncFeature);
		features.add(new ReferencesFeature(this.languageService));
		features.add(new DeclarationFeature(this.languageService));
		features.add(new HoverFeature(this.languageService));
		features.add(new DocumentSymbolFeature(this.languageService));
		features.add(new WorkspaceSymbolFeature(this.languageService, this.flixProject));
		features.add(new RenameFeature(this.languageService));
		return features;
	}

	@Override
	protected JsonRpcConnectionFactory<LanguageServer> getConnectionFactory() {
		System.out.println("FlixLanguageClient.getConnectionFactory()");
		return new AbstractJsonRpcConnectionFactory<>() {
			@Override
			protected StreamBasedConnection newStreamBasedConnection() {
				try {
					return new SocketConnection(new Socket("localhost", FlixLanguageClientController.this.port));
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
	public void dispose() {
		this.diagnosticConsumer.dispose();
		super.dispose();
	}

	public static FlixLanguageClientController connect(IFlixProject flixProject, int port) {
		System.out.println("FlixLanguageClient.connect()");
		return SafeRun.runWithResult(rollback -> {
			final FlixLanguageClientController flixLanguageClient = new FlixLanguageClientController(flixProject, port);
			rollback.add(flixLanguageClient::dispose);
			flixLanguageClient.connect();

			System.out.println("Connected language client on port " + port);

			return flixLanguageClient;
		});
	}
}
