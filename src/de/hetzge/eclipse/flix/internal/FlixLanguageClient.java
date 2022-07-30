package de.hetzge.eclipse.flix.internal;

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
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.BufferingDiagnosticConsumer;
import org.lxtk.client.CompletionFeature;
import org.lxtk.client.Feature;
import org.lxtk.client.FileOperationsFeature;
import org.lxtk.jsonrpc.AbstractJsonRpcConnectionFactory;
import org.lxtk.jsonrpc.JsonRpcConnectionFactory;
import org.lxtk.lx4e.EclipseLog;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.refactoring.FileOperationParticipantSupport;
import org.lxtk.lx4e.ui.EclipseLanguageClient;
import org.lxtk.lx4e.ui.EclipseLanguageClientController;
import org.lxtk.util.Log;
import org.lxtk.util.connect.StreamBasedConnection;

import de.hetzge.eclipse.flix.FlixCore;

public class FlixLanguageClient extends EclipseLanguageClientController<LanguageServer> {

	private final IProject project;
	private final FlixService flixService;
	private final EclipseLog log;
	private final BufferingDiagnosticConsumer diagnosticConsumer;

	public FlixLanguageClient(IProject project, FlixService flixService) {
		this.project = project;
		this.flixService = flixService;
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

//		final Collection<Feature<? super LanguageServer>> features = new ArrayList<>();
//		final TextDocumentSyncFeature textDocumentSyncFeature = new TextDocumentSyncFeature(FlixCore.DOCUMENT_SERVICE);
//		textDocumentSyncFeature.setChangeEventMergeStrategy(new EclipseTextDocumentChangeEventMergeStrategy());
//		features.add(textDocumentSyncFeature);
//		features.add(new CompletionFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new DocumentFormattingFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new DocumentRangeFormattingFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new DocumentSymbolFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new FoldingRangeFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new HoverFeature(FlixCore.LANGUAGE_SERVICE));




		final Collection<Feature<? super LanguageServer>> features = new ArrayList<>();
//		final TextDocumentSyncFeature textDocumentSyncFeature = new TextDocumentSyncFeature(FlixCore.DOCUMENT_SERVICE);
//		textDocumentSyncFeature.setChangeEventMergeStrategy(new EclipseTextDocumentChangeEventMergeStrategy());
//		features.add(textDocumentSyncFeature);
//		features.add(new DefinitionFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new DocumentFormattingFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new DocumentHighlightFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new DocumentRangeFormattingFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new DocumentSymbolFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new FoldingRangeFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new HoverFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new ImplementationFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new ReferencesFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new RenameFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new SignatureHelpFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new TypeDefinitionFeature(FlixCore.LANGUAGE_SERVICE));
//		features.add(new WorkspaceSymbolFeature(FlixCore.LANGUAGE_SERVICE, this.project));
		features.add(new CompletionFeature(FlixCore.LANGUAGE_SERVICE));
		features.add(FileOperationsFeature.newInstance(new FileOperationParticipantSupport(FlixCore.CHANGE_FACTORY)));



//		final List<Feature<? super LanguageServer>> features = List.of( //
//				FileOperationsFeature.newInstance(new FileOperationParticipantSupport(new WorkspaceEditChangeFactory(FlixCore.DOCUMENT_SERVICE))), //
//				new TextDocumentSyncFeature(FlixCore.DOCUMENT_SERVICE) //
//		);
		return new EclipseLanguageClient<>(this.log, this.diagnosticConsumer, FlixCore.CHANGE_FACTORY, features) {
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
				return FlixLanguageClient.this.flixService.getConnection();
			}
		};
	}

	@Override
	protected Log log() {
		return this.log;
	}

}
