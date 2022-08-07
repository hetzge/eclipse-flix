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
import org.lxtk.WorkspaceService;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.BufferingDiagnosticConsumer;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.DeclarationFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.FileOperationsFeature;
import org.lxtk.client.HoverFeature;
import org.lxtk.client.ReferencesFeature;
import org.lxtk.client.TextDocumentSyncFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.EclipseTextDocumentChangeEventMergeStrategy;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.lx4e.ui.EclipseLanguageClientController;
import org.lxtk.util.Log;
import org.lxtk.util.connect.SocketConnection;
import org.lxtk.util.connect.StreamBasedConnection;

import de.hetzge.eclipse.flix.Activator;
import de.hetzge.eclipse.flix.FlixCore;
import de.hetzge.eclipse.flix.FlixMarkerResolutionGenerator;
import de.hetzge.eclipse.flix.FlixService;

public class FlixLanguageClient extends EclipseLanguageClientController<LanguageServer> {

	private final IProject project;
	private final FlixService flixService;
	private final int port;
	private final EclipseLog log;
	private final BufferingDiagnosticConsumer diagnosticConsumer;

	public FlixLanguageClient(IProject project, FlixService flixService, int port) {
		this.project = project;
		this.flixService = flixService;
		this.port = port;
		this.log = new EclipseLog(Activator.getDefault().getBundle(), "flix-language-client:" + project.getName());
		this.diagnosticConsumer = new BufferingDiagnosticConsumer(new DiagnosticMarkers(FlixMarkerResolutionGenerator.MARKER_TYPE));
	}

	@Override
	protected List<DocumentFilter> getDocumentSelector() {
		return Collections.singletonList(new DocumentFilter(FlixCore.LANGUAGE_ID, "file", this.project.getLocation().append("**").toString())); //$NON-NLS-1$
	}

	@Override
	protected Class<LanguageServer> getServerInterface() {
		return LanguageServer.class;
	}

	@Override
	protected AbstractLanguageClient<LanguageServer> getLanguageClient() {

		final TextDocumentSyncFeature textDocumentSyncFeature = new TextDocumentSyncFeature(FlixCore.DOCUMENT_SERVICE);
		textDocumentSyncFeature.setChangeEventMergeStrategy(new EclipseTextDocumentChangeEventMergeStrategy());

		final Collection<Feature<? super LanguageServer>> features = new ArrayList<>();
		features.add(new CompletionFeature(FlixCore.LANGUAGE_SERVICE));
		features.add(FileOperationsFeature.newInstance(Activator.getDefault().getResourceMonitor()));
		features.add(textDocumentSyncFeature);
		features.add(new ReferencesFeature(FlixCore.LANGUAGE_SERVICE));
		features.add(new DeclarationFeature(FlixCore.LANGUAGE_SERVICE));
		features.add(new HoverFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new ExecuteCommandFeature(new EclipseCommandService()));

		return new EclipseLanguageClient<>(this.log, this.diagnosticConsumer, FlixCore.WORKSPACE_EDIT_CHANGE_FACTORY, features) {
			@Override
			public WorkspaceService getWorkspaceService() {
				return FlixCore.WORKSPACE_SERVICE;
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
}
