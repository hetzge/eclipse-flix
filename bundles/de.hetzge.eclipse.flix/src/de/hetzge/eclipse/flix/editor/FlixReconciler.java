package de.hetzge.eclipse.flix.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.CodeMiningReconciler;
import org.eclipse.jface.text.reconciler.AbstractReconciler;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
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
public class FlixReconciler extends AbstractReconciler {
	private final CodeMiningReconciler codeMiningReconciler;
	private Runnable uninstallRunnable;

	public FlixReconciler(IEditorPart editor) {
		this.codeMiningReconciler = new CodeMiningReconciler();
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
	public IReconcilingStrategy getReconcilingStrategy(String contentType) {
		return this.codeMiningReconciler.getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
	}

	@Override
	protected void process(DirtyRegion dirtyRegion) {
	}

	@Override
	protected void reconcilerDocumentChanged(IDocument newDocument) {
	}
}
