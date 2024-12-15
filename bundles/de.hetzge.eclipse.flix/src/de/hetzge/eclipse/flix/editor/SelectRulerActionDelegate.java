package de.hetzge.eclipse.flix.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.lx4e.ui.SelectAnnotationRulerAction;

import de.hetzge.eclipse.flix.FlixActivator;

public class SelectRulerActionDelegate extends AbstractRulerActionDelegate {

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		System.out.println("SelectRulerActionDelegate.createAction()");
		return new SelectAnnotationRulerAction(editor, rulerInfo, FlixActivator.getCombinedPreferenceStore());
	}
}
