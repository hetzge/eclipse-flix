package de.hetzge.eclipse.flix.client;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceService;
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
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.EclipseTextDocumentChangeEventMergeStrategy;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.lx4e.ui.EclipseLanguageClientController;
import org.lxtk.util.Log;
import org.lxtk.util.SafeRun;
import org.lxtk.util.connect.SocketConnection;
import org.lxtk.util.connect.StreamBasedConnection;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.FlixMarkerResolutionGenerator;

public class FlixLanguageClient extends EclipseLanguageClientController<LanguageServer> {

	private final IProject project;
	private final int port;
	private final EclipseLog log;
	private final BufferingDiagnosticConsumer diagnosticConsumer;

	public FlixLanguageClient(IProject project, int port) {
		this.project = project;
		this.port = port;
		this.log = new EclipseLog(FlixActivator.getDefault().getBundle(), "flix-language-client:" + project.getName());
		this.diagnosticConsumer = new BufferingDiagnosticConsumer(new DiagnosticMarkers(FlixMarkerResolutionGenerator.MARKER_TYPE));
	}

	@Override
	public void dispose() {
		this.diagnosticConsumer.dispose();
		super.dispose();
	}

	@Override
	protected List<DocumentFilter> getDocumentSelector() {
		return Collections.singletonList(new DocumentFilter(FlixConstants.LANGUAGE_ID, "file", this.project.getLocation().append("**").toString())); //$NON-NLS-1$
	}

	@Override
	protected Class<LanguageServer> getServerInterface() {
		return LanguageServer.class;
	}

	@Override
	protected AbstractLanguageClient<LanguageServer> getLanguageClient() {
		final LanguageService languageService = Flix.get().getLanguageService();

		final TextDocumentSyncFeature textDocumentSyncFeature = new TextDocumentSyncFeature(Flix.get().getDocumentService());
		textDocumentSyncFeature.setChangeEventMergeStrategy(new EclipseTextDocumentChangeEventMergeStrategy());

		final Collection<Feature<? super LanguageServer>> features = new ArrayList<>();
		features.add(new CompletionFeature(languageService));
		features.add(FileOperationsFeature.newInstance(Flix.get().getResourceMonitor()));
		features.add(textDocumentSyncFeature);
		features.add(new ReferencesFeature(languageService));
		features.add(new DeclarationFeature(languageService));
		features.add(new HoverFeature(languageService));
		features.add(new DocumentSymbolFeature(languageService));
		features.add(new RenameFeature(languageService));
//        features.add(new WorkspaceSymbolFeature(FlixCore.LANGUAGE_SERVICE, this.project));
//		features.add(new ExecuteCommandFeature(new EclipseCommandService()));

		return new EclipseLanguageClient<>(this.log, this.diagnosticConsumer, Flix.get().getChangeFactory(), features) {
			@Override
			public WorkspaceService getWorkspaceService() {
				return Flix.get().getWorkspaceService();
			}

			@Override
			protected String getMessageTitle(MessageParams params) {
				return "Flix Language Server";
			}

			@SuppressWarnings("deprecation")
			@Override
			public void fillInitializeParams(InitializeParams params) {
				super.fillInitializeParams(params);
				params.setRootUri(DocumentUri.convert(FlixLanguageClient.this.project.getLocationURI()));
			}
		};
	}

	@Override
	protected JsonRpcConnectionFactory<LanguageServer> getConnectionFactory() {
		System.out.println("FlixLanguageClient.getConnectionFactory()");
		return new AbstractJsonRpcConnectionFactory<>() {
			@Override
			protected StreamBasedConnection newStreamBasedConnection() {
				try {
					return new SocketConnection(new Socket("localhost", FlixLanguageClient.this.port));
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

	public static FlixLanguageClient connect(IProject project, int port) {
		System.out.println("FlixLanguageClient.connect()");
		return SafeRun.runWithResult(rollback -> {
			final FlixLanguageClient flixLanguageClient = new FlixLanguageClient(project, port);
			rollback.add(flixLanguageClient::dispose);
			flixLanguageClient.connect();

			System.out.println("Connected language client on port " + port);

			return flixLanguageClient;
		});
	}
}
