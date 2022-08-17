package de.hetzge.eclipse.flix.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.rename.RenameRefactoring;
import org.lxtk.lx4e.ui.refactoring.rename.AbstractRenameHandler;
import org.lxtk.lx4e.util.DefaultWordFinder;

import de.hetzge.eclipse.flix.FlixLogger;
import de.hetzge.eclipse.flix.FlixOperationTargetProvider;
import de.hetzge.eclipse.flix.FlixWorkspaceEditChangeFactory;

/**
 * A handler that starts a {@link RenameRefactoring} for Flix.
 */
public class FlixRenameHandler extends AbstractRenameHandler {
	@Override
	protected RenameRefactoring createRefactoring(LanguageOperationTarget target, IDocument document, int offset) {
		final RenameRefactoring refactoring = super.createRefactoring(target, document, offset);
		if (refactoring != null && refactoring.isApplicable()) {
			final IRegion region = DefaultWordFinder.INSTANCE.findWord(document, offset);
			if (region != null && region.getLength() > 0) {
				try {
					refactoring.setCurrentName(document.get(region.getOffset(), region.getLength()));
					return refactoring;
				} catch (final BadLocationException exception) {
					FlixLogger.logError(exception);
				}
			}
		}
		return null;
	}

	@Override
	protected LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor) {
		return FlixOperationTargetProvider.getOperationTarget(editor);
	}

	@Override
	protected IWorkspaceEditChangeFactory getWorkspaceEditChangeFactory() {
		return FlixWorkspaceEditChangeFactory.INSTANCE;
	}
}
