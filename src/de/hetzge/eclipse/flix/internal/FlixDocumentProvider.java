package de.hetzge.eclipse.flix.internal;

import java.util.List;

import org.eclipse.lsp4j.TextEdit;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.lxtk.TextDocumentSaveEvent;
import org.lxtk.TextDocumentSaveEventSource;
import org.lxtk.TextDocumentWillSaveEvent;
import org.lxtk.TextDocumentWillSaveEventSource;
import org.lxtk.TextDocumentWillSaveWaitUntilEventSource;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.WaitUntilEvent;
import org.lxtk.util.WaitUntilEventEmitter;

/**
 * Flix document provider.
 */
public class FlixDocumentProvider extends TextFileDocumentProvider implements TextDocumentWillSaveEventSource, TextDocumentWillSaveWaitUntilEventSource, TextDocumentSaveEventSource {

	private final EventEmitter<TextDocumentWillSaveEvent> onWillSaveTextDocument = new EventEmitter<>();
	private final WaitUntilEventEmitter<TextDocumentWillSaveEvent, List<TextEdit>> onWillSaveTextDocumentWaitUntil = new WaitUntilEventEmitter<>();
	private final EventEmitter<TextDocumentSaveEvent> onDidSaveTextDocument = new EventEmitter<>();

	@Override
	public EventStream<TextDocumentWillSaveEvent> onWillSaveTextDocument() {
		return onWillSaveTextDocument;
	}

	@Override
	public EventStream<WaitUntilEvent<TextDocumentWillSaveEvent, List<TextEdit>>> onWillSaveTextDocumentWaitUntil() {
		return onWillSaveTextDocumentWaitUntil;
	}

	@Override
	public EventStream<TextDocumentSaveEvent> onDidSaveTextDocument() {
		return onDidSaveTextDocument;
	}

}
