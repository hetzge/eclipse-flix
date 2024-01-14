package de.hetzge.eclipse.flix.editor.outline;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.editor.FlixEditor;

public class FlixOutlinePage extends ContentOutlinePage {

	private final FlixEditor flixEditor;
	private FlixOutlineContentProvider contentOutlineProvider;
	private final AtomicBoolean lock;

	public FlixOutlinePage(FlixEditor flixEditor) {
		System.out.println("FlixOutlinePage.FlixOutlinePage()");
		this.flixEditor = flixEditor;
		this.lock = new AtomicBoolean(false);
	}

	@Override
	public void createControl(Composite parent) {
		System.out.println("FlixOutlinePage.createControl()");
		super.createControl(parent);
		final TreeViewer viewer = getTreeViewer();
		this.contentOutlineProvider = new FlixOutlineContentProvider();
		viewer.setContentProvider(this.contentOutlineProvider);
		viewer.setLabelProvider(new FlixOutlineLabelProvider());
		viewer.setUseHashlookup(true);
		viewer.setInput(this.flixEditor.getEditorInput());
		viewer.expandAll();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (FlixOutlinePage.this.lock.get()) {
					return;
				}
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
		});
		update();
	}

	public void update() {
		if (this.flixEditor == null && this.contentOutlineProvider != null) {
			return;
		}
		final URI uri = ((IFileEditorInput) this.flixEditor.getEditorInput()).getFile().getLocationURI();
		Flix.get().getOutlineManager().get(uri).thenAccept(outline -> {
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
		final IEditorInput editorInput = this.flixEditor.getEditorInput();
		final URI uri;
		if (editorInput instanceof IFileEditorInput) {
			final IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
			uri = fileEditorInput.getFile().getLocationURI();
		} else if (editorInput instanceof IURIEditorInput) {
			final IURIEditorInput uriEditorInput = (IURIEditorInput) editorInput;
			uri = uriEditorInput.getURI();
		} else {
			return;
		}
		if (!(selection instanceof ITextSelection)) {
			return;
		}
		final ITextSelection textSelection = (ITextSelection) selection;
		final IDocument document = DefaultEditorHelper.INSTANCE.getDocument(FlixOutlinePage.this.flixEditor);
		if (document == null) {
			return;
		}
		Flix.get().getOutlineManager().getPreferedCached(uri).thenAccept(outline -> {
			final AtomicReference<TreePath> lastMatchingTreePath = new AtomicReference<>();
			final AtomicInteger smallestDistance = new AtomicInteger(Integer.MAX_VALUE);
			outline.visitPaths(path -> {
				try {
					final int startOffset = DocumentUtil.toOffset(document, path.getLast().getRange().getStart());
					final int offset = textSelection.getOffset();
					final int distance = offset - startOffset;
					if (distance >= 0 && distance < smallestDistance.get()) {
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
			Display.getDefault().asyncExec(() -> {
				this.lock.set(true);
				try {
					getTreeViewer().setSelection(new TreeSelection(treePath));
				} finally {
					this.lock.set(false);
				}
			});
		});

	}

}
