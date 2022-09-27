package de.hetzge.eclipse.flix.explorer;

import java.nio.file.Path;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.navigator.ILinkHelper;

public class FlixStandardLibraryTreeLinkHelper implements ILinkHelper {

	@Override
	public IStructuredSelection findSelection(IEditorInput anInput) {
		System.out.println("FlixStandardLibraryTreeLinkHelper.findSelection(" + anInput + ")");

		if (anInput instanceof FileStoreEditorInput) {
			// TODO what if file is not a standard library file ?!
			final FileStoreEditorInput fileStoreEditorInput = (FileStoreEditorInput) anInput;
			final FlixStandardLibraryFile standardLibraryFile = new FlixStandardLibraryFile(Path.of(fileStoreEditorInput.getURI()));
			return new TreeSelection(standardLibraryFile.getTreePath());
		}

		return null;
	}

	@Override
	public void activateEditor(IWorkbenchPage aPage, IStructuredSelection aSelection) {
		System.out.println("FlixStandardLibraryTreeLinkHelper.activateEditor(" + aSelection + ")");
		// TODO Auto-generated method stub

	}

}
