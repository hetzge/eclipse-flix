package de.hetzge.eclipse.flix.editor.outline;

import java.net.URI;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.editor.FlixEditor;

public class FlixOutlinePage extends ContentOutlinePage {

	private final FlixEditor flixEditor;
	private FlixContentOutlineProvider contentOutlineProvider;

	public FlixOutlinePage(FlixEditor flixEditor) {
		System.out.println("FlixOutlinePage.FlixOutlinePage()");
		this.flixEditor = flixEditor;
	}

	@Override
	public void createControl(Composite parent) {
		System.out.println("FlixOutlinePage.createControl()");
		super.createControl(parent);
		final TreeViewer viewer = getTreeViewer();
		this.contentOutlineProvider = new FlixContentOutlineProvider();
		viewer.setContentProvider(this.contentOutlineProvider);
		viewer.setLabelProvider(new FlixOutlineLabelProvider());
		viewer.setUseHashlookup(true);
		viewer.setInput(this.flixEditor.getEditorInput());
		viewer.expandAll();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
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
		System.out.println("FlixOutlinePage.update()");
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

}
