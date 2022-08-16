package de.hetzge.eclipse.flix.editor;

import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.outline.HandlyOutlinePage;
import org.eclipse.handly.ui.outline.ProblemMarkerListenerContribution;
import org.eclipse.handly.ui.preference.BooleanPreference;
import org.eclipse.handly.ui.preference.FlushingPreferenceStore;
import org.eclipse.handly.ui.preference.IBooleanPreference;
import org.eclipse.handly.ui.viewer.DeferredElementTreeContentProvider;
import org.eclipse.handly.ui.viewer.ProblemMarkerLabelDecorator;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IEditorPart;
import org.lxtk.lx4e.ui.LanguageElementLabelProvider;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixInputElementProvider;

/**
 * The content outline page of the Flix editor.
 */
public class FlixOutlinePage extends HandlyOutlinePage {

	public FlixOutlinePage(IEditorPart editor) {
		init(editor);
	}

	@Override
	public void dispose() {
		final IEditorPart editor = getEditor();
		if (editor instanceof FlixEditor) {
			((FlixEditor) editor).closeOutlinePage();
		}
		super.dispose();
	}

	@Override
	public IBooleanPreference getLinkWithEditorPreference() {
		return LinkWithEditorPreference.INSTANCE;
	}

	@Override
	public IBooleanPreference getLexicalSortPreference() {
		return null;
	}

	@Override
	protected void addOutlineContributions() {
		super.addOutlineContributions();
		addOutlineContribution(new ProblemMarkerListenerContribution());
	}

	@Override
	protected IInputElementProvider getInputElementProvider() {
		return FlixInputElementProvider.INSTANCE;
	}

	@Override
	protected void addElementChangeListener(IElementChangeListener listener) {
		System.out.println("FlixOutlinePage.addElementChangeListener()");
		Flix.get().getModelManager().getNotificationManager().addElementChangeListener(listener);
	}

	@Override
	protected void removeElementChangeListener(IElementChangeListener listener) {
		System.out.println("FlixOutlinePage.removeElementChangeListener()");
		Flix.get().getModelManager().getNotificationManager().removeElementChangeListener(listener);
	}

	@Override
	protected ITreeContentProvider getContentProvider() {
		System.out.println("FlixOutlinePage.getContentProvider()");
		return new DeferredElementTreeContentProvider(getTreeViewer(), null);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new DecoratingStyledCellLabelProvider(new LanguageElementLabelProvider(), new ProblemMarkerLabelDecorator(), null);
	}

	private static class LinkWithEditorPreference extends BooleanPreference {
		static final LinkWithEditorPreference INSTANCE = new LinkWithEditorPreference();

		LinkWithEditorPreference() {
			super("FlixOutline.LinkWithEditor", new FlushingPreferenceStore(FlixActivator.getDefault().getPreferenceStore())); //$NON-NLS-1$
			setDefault(true);
		}
	}
}
