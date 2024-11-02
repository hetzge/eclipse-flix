package de.hetzge.eclipse.toml;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.tomlj.Toml;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;

public class TomlSourceViewerConfiguration extends SourceViewerConfiguration {

	private final ITextEditor editor;

	public TomlSourceViewerConfiguration(ITextEditor editor) {
		this.editor = editor;
	}

	private IResource getResource() {
		final IEditorInput input = this.editor.getEditorInput();
		if (input instanceof IFileEditorInput) {
			final IFileEditorInput fileInput = (IFileEditorInput) input;
			return fileInput.getFile();
		}
		return null;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer viewer) {
		return new TMPresentationReconciler();
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		final Reconciler reconciler = new Reconciler();
		reconciler.setReconcilingStrategy(new IReconcilingStrategy() {

			@Override
			public void setDocument(IDocument document) {
			}

			@Override
			public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
				validate();
			}

			@Override
			public void reconcile(IRegion partition) {
				validate();
			}

			private void validate() {
				final TomlParseResult result = Toml.parse(sourceViewer.getDocument().get());
				final IResource resource = getResource();
				PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
					try {
						resource.deleteMarkers(IMarker.PROBLEM, true, 0);
					} catch (final CoreException exception) {
						throw new RuntimeException("Failed to clear TOML markers", exception); //$NON-NLS-1$
					}
					for (final TomlParseError error : result.errors()) {
						try {
							reportError(resource, error);
						} catch (final CoreException exception) {
							throw new RuntimeException("Failed to create TOML marker", exception); //$NON-NLS-1$
						}
					}
				});
			}
		}, IDocument.DEFAULT_CONTENT_TYPE);
		return reconciler;
	}

	private static void reportError(IResource resource, TomlParseError error) throws CoreException {
		final IMarker marker = resource.createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.LINE_NUMBER, error.position().line());
		marker.setAttribute(IMarker.LOCATION, error.getMessage());
		marker.setAttribute(IMarker.MESSAGE, error.getMessage());
		marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
	}
}
