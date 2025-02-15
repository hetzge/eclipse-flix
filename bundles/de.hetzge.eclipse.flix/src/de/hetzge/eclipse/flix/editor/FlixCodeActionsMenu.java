package de.hetzge.eclipse.flix.editor;

import java.util.List;

import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.codeaction.AbstractCodeActionMenu;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixOperationTargetProvider;

public class FlixCodeActionsMenu extends AbstractCodeActionMenu {

	@Override
	protected LanguageOperationTarget getLanguageOperationTarget() {
		return FlixOperationTargetProvider.getOperationTarget((IEditorPart) getActivePart());
	}

	@Override
	protected IWorkspaceEditChangeFactory getWorkspaceEditChangeFactory() {
		return Flix.get().getChangeFactory();
	}

	@Override
	protected List<String> getCodeActionKinds() {
		return null;
	}

}
