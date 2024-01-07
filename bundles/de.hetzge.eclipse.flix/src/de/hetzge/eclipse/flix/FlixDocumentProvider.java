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
import org.eclipse.jface.text.IDocument;
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
			final FileInfo fileInfo = getFileInfo(element);
			final IDocument document = fileInfo.fTextFileBuffer.getDocument();
			final URI uri = fileEditorInput.getFile().getLocationURI();
			SafeRun.run(rollback -> {
				EclipseTextDocument eclipseTextDocument;
				try {
					eclipseTextDocument = new EclipseTextDocument(uri, FlixConstants.LANGUAGE_ID, TextFileBuffer.forFile(fileEditorInput.getFile()), element);

					rollback.add(eclipseTextDocument::dispose);
					rollback.add(this.documentService.addTextDocument(eclipseTextDocument)::dispose);
					this.openResources.put(uri, rollback::run);
				} catch (final CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} else {
			super.connect(element);
		}
	}

	@Override
	public void disconnect(Object element) {
		if (element instanceof IFileEditorInput) {
			final IFileEditorInput fileEditorInput = (IFileEditorInput) element;
			super.disconnect(element);
			final URI uri = fileEditorInput.getFile().getLocationURI();
			final Disposable disposable = this.openResources.get(uri);
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

//	/**
//	 * An adapter from {@link IDocument} to {@link TextDocument}
//	 */
//	private static class EclipseTextDocument implements TextDocument, Disposable {
//
//		private final EventEmitter<TextDocumentChangeEvent> onWillChange;
//		private final EventEmitter<TextDocumentChangeEvent> onDidChange;
//		private final AtomicReference<TextDocumentChangeEvent> lastChangeAtomicReference;
//		private final IDocument document;
//		private final URI uri;
//		private TextDocumentContentChangeEvent currentChangeEvent;
//		private Rollback rollback;
//
//		public EclipseTextDocument(IDocument document, URI uri) {
//			this.onWillChange = new EventEmitter<>();
//			this.onDidChange = new EventEmitter<>();
//			this.lastChangeAtomicReference = new AtomicReference<>();
//			this.document = document;
//			this.uri = uri;
//			SafeRun.run(rollback -> {
//				final DocumentListener listener = new DocumentListener();
//				document.addDocumentListener(listener);
//				rollback.add(() -> document.removeDocumentListener(listener));
//				rollback.add(() -> this.onWillChange.dispose());
//				rollback.add(() -> this.onDidChange.dispose());
//				this.rollback = rollback;
//			});
//			this.lastChangeAtomicReference.compareAndSet(null, new TextDocumentChangeEvent(new DefaultTextDocumentSnapshot(this, 0, document.get()), Collections.emptyList()));
//		}
//
//		@Override
//		public void dispose() {
//			this.rollback.run();
//		}
//
//		@Override
//		public EventStream<TextDocumentChangeEvent> onWillChange() {
//			return this.onWillChange;
//		}
//
//		@Override
//		public EventStream<TextDocumentChangeEvent> onDidChange() {
//			return this.onDidChange;
//		}
//
//		@Override
//		public URI getUri() {
//			return this.uri;
//		}
//
//		@Override
//		public TextDocumentChangeEvent getLastChange() {
//			return this.lastChangeAtomicReference.get();
//		}
//
//		@Override
//		public String getLanguageId() {
//			return FlixConstants.LANGUAGE_ID;
//		}
//
//		private TextDocumentChangeEvent newChangeEvent(TextDocumentContentChangeEvent event, DocumentEvent originalEvent, boolean unprocessed) {
//			final TextDocumentChangeEvent lastEvent = this.lastChangeAtomicReference.get();
//			final int lastVersion = (lastEvent == null) ? 0 : lastEvent.getSnapshot().getVersion();
//			final TextDocumentSnapshot snapshot = new DefaultTextDocumentSnapshot(this, unprocessed ? lastVersion : lastVersion + 1, this.document.get());
//			if (originalEvent.getModificationStamp() != getModificationStamp()) {
//				throw new IllegalStateException("Illegal modification timestamp");
//			}
//			return new TextDocumentChangeEvent(snapshot, Collections.singletonList(event));
//		}
//
//		public long getModificationStamp() {
//			if (this.document instanceof IDocumentExtension4) {
//				return ((IDocumentExtension4) this.document).getModificationStamp();
//			}
//			return IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
//		}
//
//		private TextDocumentContentChangeEvent newContentChangeEvent(DocumentEvent event) {
//			try {
//				final Range range = DocumentUtil.toRange(this.document, event.getOffset(), event.getLength());
//				return new TextDocumentContentChangeEvent(range, event.getText());
//			} catch (final BadLocationException exception) {
//				throw new IllegalStateException(exception);
//			}
//		}
//
//		private class DocumentListener implements IDocumentListener {
//			@Override
//			public void documentAboutToBeChanged(DocumentEvent event) {
//				EclipseTextDocument.this.currentChangeEvent = newContentChangeEvent(event);
//				final TextDocumentChangeEvent newChangeEvent = newChangeEvent(EclipseTextDocument.this.currentChangeEvent, event, true);
//				EclipseTextDocument.this.onWillChange.emit(newChangeEvent, FlixLogger::logError);
//			}
//
//			@Override
//			public void documentChanged(DocumentEvent event) {
//				try {
//					final TextDocumentChangeEvent newChangeEvent = newChangeEvent(EclipseTextDocument.this.currentChangeEvent, event, false);
//					EclipseTextDocument.this.onDidChange.emit(newChangeEvent, FlixLogger::logError);
//					EclipseTextDocument.this.lastChangeAtomicReference.set(newChangeEvent);
//				} finally {
//					EclipseTextDocument.this.currentChangeEvent = null;
//				}
//			}
//		}
//	}
}
