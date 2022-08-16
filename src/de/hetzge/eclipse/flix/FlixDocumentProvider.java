package de.hetzge.eclipse.flix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.TextFileBuffer;
import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.ui.texteditor.SourceFileDocumentProvider;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.lsp4j.TextDocumentSaveReason;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.IEditorInput;
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
public class FlixDocumentProvider extends SourceFileDocumentProvider implements TextDocumentWillSaveEventSource, TextDocumentWillSaveWaitUntilEventSource, TextDocumentSaveEventSource {
	private final EventEmitter<TextDocumentWillSaveEvent> onWillSaveTextDocument = new EventEmitter<>();
	private final WaitUntilEventEmitter<TextDocumentWillSaveEvent, List<TextEdit>> onWillSaveTextDocumentWaitUntil = new WaitUntilEventEmitter<>();
	private final EventEmitter<TextDocumentSaveEvent> onDidSaveTextDocument = new EventEmitter<>();

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
	protected FileInfo createEmptyFileInfo() {
		return new XFileInfo();
	}

	@Override
	protected FileInfo createFileInfo(Object element) throws CoreException {
		final XFileInfo info = (XFileInfo) super.createFileInfo(element);
		if (info == null || info.fTextFileBuffer == null) {
			return null;
		}

		try (TextFileBuffer buffer = info.fTextFileBufferLocationKind == null ? TextFileBuffer.forFileStore(info.fTextFileBuffer.getFileStore()) : TextFileBuffer.forLocation(info.fTextFileBuffer.getLocation(), info.fTextFileBufferLocationKind);) {
			SafeRun.run(rollback -> {
				final EclipseTextDocument document = new EclipseTextDocument(info.fTextFileBuffer.getFileStore().toURI(), FlixConstants.LANGUAGE_ID, buffer, element);
				rollback.add(document::dispose);

				final Disposable registration = Flix.get().getDocumentService().addTextDocument(document);
				rollback.add(registration::dispose);

				rollback.setLogger(FlixLogger::logError);
				info.disposeRunnable = rollback;
			});
		}
		return info;
	}

	@Override
	protected void disposeFileInfo(Object element, FileInfo info) {
		try {
			final XFileInfo xInfo = (XFileInfo) info;
			if (xInfo.disposeRunnable != null) {
				xInfo.disposeRunnable.run();
			}
		} finally {
			super.disposeFileInfo(element, info);
		}
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

	private static class XFileInfo extends FileInfo {
		Runnable disposeRunnable;
	}

	@Override
	protected ISourceFile getSourceFile(Object element) {
		if (!(element instanceof IEditorInput)) {
			return null;
		}
		return FlixInputElementProvider.INSTANCE.getElement((IEditorInput) element);
	}
}
