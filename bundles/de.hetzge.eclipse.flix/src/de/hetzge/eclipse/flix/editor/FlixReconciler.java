package de.hetzge.eclipse.flix.editor;

import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.ui.IWorkingCopyManager;
import org.eclipse.handly.ui.text.reconciler.CompositeReconcilingStrategy;
import org.eclipse.handly.ui.text.reconciler.EditorWorkingCopyReconciler;
import org.eclipse.handly.ui.text.reconciler.WorkingCopyReconcilingStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.CodeMiningReconciler;
import org.eclipse.ui.IEditorPart;
import org.lxtk.DocumentSymbolProvider;
import org.lxtk.util.Registry;
import org.lxtk.util.SafeRun;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixLogger;

/**
 * Reconciler for a Flix-specific text editor.
 *
 * @see https://wiki.eclipse.org/FAQ_How_do_I_use_a_model_reconciler%3F
 */
public class FlixReconciler extends EditorWorkingCopyReconciler {
	private final CodeMiningReconciler codeMiningReconciler;
	private Runnable uninstallRunnable;

	public FlixReconciler(IEditorPart editor, IWorkingCopyManager workingCopyManager) {
		super(editor, workingCopyManager);
		this.codeMiningReconciler = new CodeMiningReconciler();
		setReconcilingStrategy(new CompositeReconcilingStrategy( //
				new WorkingCopyReconcilingStrategy(workingCopyManager::getWorkingCopy), //
				this.codeMiningReconciler.getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE)));
	}

	@Override
	public void install(ITextViewer textViewer) {
		super.install(textViewer);

		SafeRun.run(rollback -> {
			final Registry<DocumentSymbolProvider> providers = Flix.get().getLanguageService().getDocumentSymbolProviders();
			rollback.add(providers.onDidAdd().subscribe(provider -> forceReconciling())::dispose);
			rollback.add(providers.onDidRemove().subscribe(provider -> forceReconciling())::dispose);
			rollback.setLogger(FlixLogger::logError);
			this.uninstallRunnable = rollback;
		});

		this.codeMiningReconciler.install(textViewer);
	}

	@Override
	public void uninstall() {
		try {
			if (this.uninstallRunnable != null) {
				this.uninstallRunnable.run();
			}
		} finally {
			super.uninstall();
			this.codeMiningReconciler.uninstall();
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
