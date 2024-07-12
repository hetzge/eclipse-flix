package de.hetzge.eclipse.flix.editor;

import java.net.URI;
import java.time.Duration;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.Throttler;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm4e.languageconfiguration.internal.LanguageConfigurationCharacterPairMatcher;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.ui.folding.FoldingManager;
import org.lxtk.lx4e.ui.highlight.Highlighter;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixActivator;
import de.hetzge.eclipse.flix.FlixDocumentProvider;
import de.hetzge.eclipse.flix.FlixOperationTargetProvider;
import de.hetzge.eclipse.flix.editor.outline.FlixOutlinePage;

public class FlixEditor extends AbstractDecoratedTextEditor {
	private static final String MATCHING_BRACKETS_PREFERENCES_KEY = "matchingBrackets";
	private static final String MATCHING_BRACKETS_COLOR_PREFERENCES_KEY = "matchingBracketsColor";
	private static final String HIGHLIGHT_BRACKET_AT_CARET_LOCATION_PREFERENCES_KEY = "highlightBracketAtCaretLocation";
	private static final String ENCLOSING_BRACKETS_PREFERENCES_KEY = "enclosingBrackets";

	private final FlixOutlinePage outlinePage;
	private final Throttler syncOutlineThrottler;
	private ProjectionSupport projectionSupport;
	private FoldingManager foldingManager;
	private Highlighter highlighter;

	public FlixEditor() {
		this.syncOutlineThrottler = new Throttler(PlatformUI.getWorkbench().getDisplay(), Duration.ofMillis(250), this::syncOutline);
		this.outlinePage = new FlixOutlinePage(this);
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		final ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
		this.projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		this.projectionSupport.install();
		this.foldingManager = new FoldingManager(viewer, () -> FlixOperationTargetProvider.getOperationTarget(this));
		this.foldingManager.install();
		this.highlighter = new Highlighter(viewer, getSelectionProvider(), () -> FlixOperationTargetProvider.getOperationTarget(this));
		this.highlighter.install();
		viewer.doOperation(ProjectionViewer.TOGGLE);
	}

	@Override
	protected void initializeEditor() {
		final FlixDocumentProvider documentProvider = Flix.get().getDocumentProvider();
		setPreferenceStore(getPreferenceStores());
		setDocumentProvider(documentProvider);
		setSourceViewerConfiguration(new FlixSourceViewerConfiguration(getPreferenceStores(), this));
		setEditorContextMenuId("#FlixEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#FlixRulerContext"); //$NON-NLS-1$
	}

	@Override
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		final ISourceViewer sourceViewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);

		final SourceViewerDecorationSupport support = getSourceViewerDecorationSupport(sourceViewer);
		support.setCharacterPairMatcher(new LanguageConfigurationCharacterPairMatcher());
		support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS_PREFERENCES_KEY, MATCHING_BRACKETS_COLOR_PREFERENCES_KEY, HIGHLIGHT_BRACKET_AT_CARET_LOCATION_PREFERENCES_KEY, ENCLOSING_BRACKETS_PREFERENCES_KEY);

		return sourceViewer;
	}

	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		this.syncOutlineThrottler.throttledExec();
	}

	private void syncOutline() {
		this.outlinePage.update(getSelectionProvider().getSelection());
	}

	private ChainedPreferenceStore getPreferenceStores() {
		return new ChainedPreferenceStore(new IPreferenceStore[] { FlixActivator.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
	}

	@Override
	public void dispose() {
		try {
			if (this.highlighter != null) {
				this.highlighter.uninstall();
				this.highlighter.dispose();
				this.highlighter = null;
			}
			if (this.foldingManager != null) {
				this.foldingManager.uninstall();
				this.foldingManager = null;
			}
			if (this.projectionSupport != null) {
				this.projectionSupport.dispose();
				this.projectionSupport = null;
			}
		} finally {
			super.dispose();
		}
	}

	public URI getUri() {
		final IEditorInput editorInput = this.getEditorInput();
		if (!(editorInput instanceof IURIEditorInput)) {
			throw new RuntimeException("Unsupported editor input: " + editorInput.getClass().getName()); //$NON-NLS-1$
		}
		final IURIEditorInput uriEditorInput = (IURIEditorInput) editorInput;
		return uriEditorInput.getURI();
	}

	@Override
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "de.hetzge.eclipse.flix.editor.scope" }); //$NON-NLS-1$
	}

	@Override
	protected void editorSaved() {
		super.editorSaved();
		this.outlinePage.update();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IContentOutlinePage.class) {
			return adapter.cast(this.outlinePage);
		} else if (adapter == LanguageOperationTarget.class) {
			return adapter.cast(FlixOperationTargetProvider.getOperationTarget(this));
		} else {
			return super.getAdapter(adapter);
		}
	}

	public static void initPreferencesStore(IPreferenceStore preferenceStore) {
		preferenceStore.setDefault(MATCHING_BRACKETS_PREFERENCES_KEY, true);
		preferenceStore.setDefault(MATCHING_BRACKETS_COLOR_PREFERENCES_KEY, StringConverter.asString(new RGB(255, 0, 0)));
		preferenceStore.setDefault(HIGHLIGHT_BRACKET_AT_CARET_LOCATION_PREFERENCES_KEY, true);
		preferenceStore.setDefault(ENCLOSING_BRACKETS_PREFERENCES_KEY, true);
	}
}
