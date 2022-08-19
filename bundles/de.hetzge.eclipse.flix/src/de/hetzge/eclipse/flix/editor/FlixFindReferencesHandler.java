package de.hetzge.eclipse.flix.editor;

import org.eclipse.ui.IEditorPart;
import org.lxtk.DocumentService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.ui.references.AbstractFindReferencesHandler;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixOperationTargetProvider;

public class FlixFindReferencesHandler extends AbstractFindReferencesHandler {

	@Override
	protected LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor) {
		return FlixOperationTargetProvider.getOperationTarget(editor);
	}

	@Override
	protected DocumentService getDocumentService() {
		return Flix.get().getDocumentService();
	}
}
