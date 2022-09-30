package de.hetzge.eclipse.flix.explorer;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.navigator.ILinkHelper;

import de.hetzge.eclipse.flix.model.impl.FlixSourceFile;

public class FlixStandardLibraryTreeLinkHelper implements ILinkHelper {

	@Override
	public IStructuredSelection findSelection(IEditorInput anInput) {
		System.out.println("FlixStandardLibraryTreeLinkHelper.findSelection(" + anInput + ")");

		if (anInput instanceof FileStoreEditorInput) {
			// TODO what if file is not a standard library file ?!
			final FileStoreEditorInput fileStoreEditorInput = (FileStoreEditorInput) anInput;
			return new TreeSelection(new TreePath(new Object[] { new FlixSourceFile(fileStoreEditorInput.getURI()) })); // TODO dont use impl
		}

		return null;
	}

	@Override
	public void activateEditor(IWorkbenchPage aPage, IStructuredSelection aSelection) {
		System.out.println("FlixStandardLibraryTreeLinkHelper.activateEditor(" + aSelection + ")");
		// TODO Auto-generated method stub

	}

}
