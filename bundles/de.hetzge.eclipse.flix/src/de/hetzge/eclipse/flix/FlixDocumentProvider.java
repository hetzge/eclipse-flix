package de.hetzge.eclipse.flix;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.lsp4j.TextDocumentSaveReason;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.lxtk.DocumentService;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentSaveEvent;
import org.lxtk.TextDocumentSaveEventSource;
import org.lxtk.TextDocumentWillSaveEvent;
import org.lxtk.TextDocumentWillSaveEventSource;
import org.lxtk.TextDocumentWillSaveWaitUntilEventSource;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.util.Disposable;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.SafeRun;
import org.lxtk.util.WaitUntilEvent;
import org.lxtk.util.WaitUntilEventEmitter;

/**
 * Flix document provider.
 */
public class FlixDocumentProvider extends TextFileDocumentProvider implements TextDocumentWillSaveEventSource, TextDocumentWillSaveWaitUntilEventSource, TextDocumentSaveEventSource {
	private final EventEmitter<TextDocumentWillSaveEvent> onWillSaveTextDocument;
	private final WaitUntilEventEmitter<TextDocumentWillSaveEvent, List<TextEdit>> onWillSaveTextDocumentWaitUntil;
	private final EventEmitter<TextDocumentSaveEvent> onDidSaveTextDocument;
	private final Map<URI, Disposable> openResources;
	private final DocumentService documentService;

	public FlixDocumentProvider(DocumentService documentService) {
		this.documentService = documentService;
		this.onWillSaveTextDocument = new EventEmitter<>();
		this.onWillSaveTextDocumentWaitUntil = new WaitUntilEventEmitter<>();
		this.onDidSaveTextDocument = new EventEmitter<>();
		this.openResources = new ConcurrentHashMap<>();
	}

	@Override
	public void connect(Object element) throws CoreException {
		if (element instanceof IFileEditorInput) {
			final IFileEditorInput fileEditorInput = (IFileEditorInput) element;
			super.connect(element);
			final URI uri = fileEditorInput.getFile().getLocationURI();
			SafeRun.run(rollback -> {
				final TextFileBuffer buffer = createBuffer(fileEditorInput);
				final EclipseTextDocument eclipseTextDocument = new EclipseTextDocument(uri, FlixConstants.LANGUAGE_ID, buffer, element);
				rollback.add(eclipseTextDocument::dispose);
				rollback.add(this.documentService.addTextDocument(eclipseTextDocument)::dispose);
				this.openResources.put(uri, rollback::run);
			});
		} else {
			super.connect(element);
		}
	}

	private TextFileBuffer createBuffer(final IFileEditorInput fileEditorInput) {
		try {
			return TextFileBuffer.forFile(fileEditorInput.getFile());
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void disconnect(Object element) {
		if (element instanceof IFileEditorInput) {
			final IFileEditorInput fileEditorInput = (IFileEditorInput) element;
			super.disconnect(element);
			final URI uri = fileEditorInput.getFile().getLocationURI();
			final Disposable disposable = this.openResources.remove(uri);
			if (disposable != null) {
				disposable.dispose();
			}
		}
	}

	@Override
	public EventStream<TextDocumentWillSaveEvent> onWillSaveTextDocument() {
		return this.onWillSaveTextDocument;
	}

	@Override
	public EventStream<WaitUntilEvent<TextDocumentWillSaveEvent, List<TextEdit>>> onWillSaveTextDocumentWaitUntil() {
		return this.onWillSaveTextDocumentWaitUntil;
	}

	@Override
	public EventStream<TextDocumentSaveEvent> onDidSaveTextDocument() {
		return this.onDidSaveTextDocument;
	}

	@Override
	protected void commitFileBuffer(IProgressMonitor monitor, FileInfo info, boolean overwrite) throws CoreException {
		final TextDocument document = Flix.get().getDocumentService().getTextDocument(info.fTextFileBuffer.getFileStore().toURI());

		if (document != null) {
			final TextDocumentWillSaveEvent event = new TextDocumentWillSaveEvent(document, TextDocumentSaveReason.Manual);

			this.onWillSaveTextDocument.emit(event, FlixLogger::logError);

			final CompletableFuture<List<List<TextEdit>>> future = this.onWillSaveTextDocumentWaitUntil.emit(event, FlixLogger::logError);
			List<List<TextEdit>> result = null;
			try {
				result = future.get(1500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				FlixLogger.logError(e);
			}
			if (result != null && !result.isEmpty()) {
				final List<TextEdit> edits = new ArrayList<>();
				result.forEach(edits::addAll);
				try {
					DocumentUtil.applyEdits(info.fTextFileBuffer.getDocument(), edits);
				} catch (MalformedTreeException | BadLocationException e) {
					FlixLogger.logError(e);
				}
			}
		}

		super.commitFileBuffer(monitor, info, overwrite);

		if (document != null) {
			this.onDidSaveTextDocument.emit(new TextDocumentSaveEvent(document, info.fTextFileBuffer.getDocument().get()), FlixLogger::logError);
		}
	}
}
