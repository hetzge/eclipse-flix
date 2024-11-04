package de.hetzge.eclipse.flix.editor.outline;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.editor.FlixEditor;

public class FlixOutlinePage extends ContentOutlinePage {

	private final FlixEditor flixEditor;
	private FlixOutlineContentProvider contentOutlineProvider;
	private DocumentSymbol last;

	public FlixOutlinePage(FlixEditor flixEditor) {
		this.flixEditor = flixEditor;
		this.last = null;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		final TreeViewer viewer = getTreeViewer();
		this.contentOutlineProvider = new FlixOutlineContentProvider();
		viewer.setContentProvider(this.contentOutlineProvider);
		viewer.setLabelProvider(new FlixOutlineLabelProvider());
		viewer.setUseHashlookup(true);
		viewer.setInput(this.flixEditor.getEditorInput());
		viewer.expandAll();
		viewer.addSelectionChangedListener(this::onSelectionChanged);
		update();
	}

	public void update() {
		if (this.flixEditor == null || this.contentOutlineProvider == null) {
			return;
		}
		Flix.get().getOutlineManager().queryOutline(this.flixEditor.getUri()).thenAccept(outline -> {
			Display.getDefault().asyncExec(() -> {
				this.contentOutlineProvider.setRootSymbols(outline.getRootSymbols());
				final TreeViewer viewer = getTreeViewer();
				if (viewer != null) {
					final Control control = viewer.getControl();
					if (control != null && !control.isDisposed()) {
						control.setRedraw(false);
						viewer.setInput(this.flixEditor.getEditorInput());
						control.setRedraw(true);
						viewer.expandAll();
					}
				}
			});
		});
	}

	public void update(ISelection selection) {
		if (this.flixEditor == null && this.contentOutlineProvider != null) {
			return;
		}
		final URI uri = this.flixEditor.getUri();
		if (!(selection instanceof ITextSelection)) {
			return;
		}
		final ITextSelection textSelection = (ITextSelection) selection;
		final IDocument document = DefaultEditorHelper.INSTANCE.getDocument(FlixOutlinePage.this.flixEditor);
		if (document == null) {
			return;
		}
		Flix.get().getOutlineManager().queryOutlinePreferCache(uri).thenAccept(outline -> {
			final AtomicReference<TreePath> lastMatchingTreePath = new AtomicReference<>();
			final AtomicInteger smallestDistance = new AtomicInteger(Integer.MAX_VALUE);
			outline.visitPaths(path -> {
				try {
					final int startOffset = DocumentUtil.toOffset(document, path.getLast().getRange().getStart());
					final int distance = textSelection.getOffset() - startOffset;
					if (distance >= 0 && distance <= smallestDistance.get()) {
						smallestDistance.set(distance);
						lastMatchingTreePath.set(new TreePath(path.toArray()));
					}
				} catch (final BadLocationException exception) {
					throw new RuntimeException(exception);
				}
			});
			final TreePath treePath = lastMatchingTreePath.get();
			if (treePath == null) {
				return;
			}
			if (Objects.equals(treePath.getLastSegment(), this.last)) {
				return;
			}
			this.last = (DocumentSymbol) treePath.getLastSegment();
			if (getTreeViewer() != null) {
				Display.getDefault().asyncExec(() -> {
					getTreeViewer().setSelection(new TreeSelection(treePath));
				});
			}
		});
	}

	private void onSelectionChanged(SelectionChangedEvent event) {
		final ISelection selection = event.getSelection();
		if (!(selection instanceof ITreeSelection)) {
			return;
		}
		final ITreeSelection treeSelection = (ITreeSelection) selection;
		if (treeSelection.getPaths().length == 0) {
			return;
		}
		final Object lastSegment = treeSelection.getPaths()[0].getLastSegment();
		if (!(lastSegment instanceof DocumentSymbol)) {
			return;
		}
		if (Objects.equals(lastSegment, this.last)) {
			return;
		}
		this.last = (DocumentSymbol) lastSegment;
		final DocumentSymbol documentSymbol = (DocumentSymbol) lastSegment;
		final IDocument document = DefaultEditorHelper.INSTANCE.getDocument(FlixOutlinePage.this.flixEditor);
		if (document == null) {
			return;
		}
		try {
			final int startOffset = DocumentUtil.toOffset(document, documentSymbol.getSelectionRange().getStart());
			final int endOffset = DocumentUtil.toOffset(document, documentSymbol.getSelectionRange().getEnd());
			FlixOutlinePage.this.flixEditor.selectAndReveal(startOffset, endOffset - startOffset);
		} catch (final BadLocationException exception) {
			throw new RuntimeException(exception);
		}
	}
}
