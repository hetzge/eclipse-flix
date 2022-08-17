package de.hetzge.eclipse.flix.editor;

import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.quickoutline.HandlyOutlinePopup;
import org.eclipse.handly.ui.viewer.DeferredElementTreeContentProvider;
import org.eclipse.handly.ui.viewer.ProblemMarkerLabelDecorator;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.lxtk.lx4e.ui.LanguageElementLabelProvider;

import de.hetzge.eclipse.flix.FlixInputElementProvider;

/**
 * The outline popup of the Flix editor.
 */
public class FlixOutlinePopup extends HandlyOutlinePopup {
	@Override
	protected IInputElementProvider getInputElementProvider() {
		return FlixInputElementProvider.INSTANCE;
	}

	@Override
	protected ITreeContentProvider getContentProvider() {
		return new DeferredElementTreeContentProvider(getTreeViewer(), null);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new DecoratingStyledCellLabelProvider(new LanguageElementLabelProvider(), new ProblemMarkerLabelDecorator(), null);
	}
}
