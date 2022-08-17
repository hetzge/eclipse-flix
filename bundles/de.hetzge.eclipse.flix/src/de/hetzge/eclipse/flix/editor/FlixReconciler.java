package de.hetzge.eclipse.flix.editor;

import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.ui.IWorkingCopyManager;
import org.eclipse.handly.ui.text.reconciler.EditorWorkingCopyReconciler;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.lxtk.DocumentSymbolProvider;
import org.lxtk.util.Registry;
import org.lxtk.util.SafeRun;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixLogger;

/**
 * Reconciler for a Flix-specific text editor.
 */
public class FlixReconciler extends EditorWorkingCopyReconciler {
	private Runnable uninstallRunnable;

	public FlixReconciler(IEditorPart editor, IWorkingCopyManager workingCopyManager) {
		super(editor, workingCopyManager);
	}

	@Override
	public void install(ITextViewer textViewer) {
		super.install(textViewer);

		SafeRun.run(rollback -> {
			final Registry<DocumentSymbolProvider> providers = Flix.get().getLanguageService().getDocumentSymbolProviders();
			rollback.add(providers.onDidAdd().subscribe(provider -> forceReconciling())::dispose);
			rollback.add(providers.onDidRemove().subscribe(provider -> forceReconciling())::dispose);

			rollback.setLogger(exception -> FlixLogger.logError(exception));
			this.uninstallRunnable = rollback;
		});
	}

	@Override
	public void uninstall() {
		try {
			if (this.uninstallRunnable != null) {
				this.uninstallRunnable.run();
			}
		} finally {
			super.uninstall();
		}
	}

	@Override
	protected void addElementChangeListener(IElementChangeListener listener) {
		Flix.get().getModelManager().getNotificationManager().addElementChangeListener(listener);
	}

	@Override
	protected void removeElementChangeListener(IElementChangeListener listener) {
		Flix.get().getModelManager().getNotificationManager().removeElementChangeListener(listener);
	}
}
