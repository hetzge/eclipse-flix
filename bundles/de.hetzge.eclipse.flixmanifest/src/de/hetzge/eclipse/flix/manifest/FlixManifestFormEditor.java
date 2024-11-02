package de.hetzge.eclipse.flix.manifest;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.Throttler;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import de.hetzge.eclipse.flix.manifest.FlixManifestToml.FlixDependency;
import de.hetzge.eclipse.flix.manifest.FlixManifestToml.MavenDependency;
import de.hetzge.eclipse.toml.TomlEditor;

public class FlixManifestFormEditor extends SharedHeaderFormEditor {

	private TomlEditor editor;
	private FlixManifestFormPage formPage;
	private int editorPageIndex;
	private int formPageIndex;

	@Override
	protected void addPages() {
		try {
			this.editor = new TomlEditor();
			this.editorPageIndex = addPage(this.editor, getEditorInput());
			setPageText(this.editorPageIndex, "Toml");
			this.formPage = new FlixManifestFormPage(this, "FORM", "Form");
			this.formPageIndex = addPage(this.formPage);
		} catch (final PartInitException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public String getPartName() {
		final IEditorInput editorInput = getEditorInput();
		if (!(editorInput instanceof IFileEditorInput)) {
			return super.getPartName();
		}
		final IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
		final IProject project = fileEditorInput.getFile().getProject();
		final String projectName = project.getName();
		return "Flix project: " + projectName;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		this.editor.doSave(monitor);
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		System.out.println("FlixTomlFormEditor.isDirty()");
		return super.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (this.formPage.getPartControl() != null) {
			if (newPageIndex == this.formPageIndex) {
				this.formPage.syncFromEditor();
			} else if (newPageIndex == this.editorPageIndex) {
				this.formPage.syncToEditor();
			}
		}
	}

	private final class FlixManifestFormPage extends FormPage {
		private final Throttler syncEditorThrottler;
		private Text nameText;
		private Text descriptionText;
		private Text projectVersion;
		private Text flixVersion;
		private DependencyTableViewer dependenciesTableViewer;
		private MavenDependencyTableViewer mavenDependenciesTableViewer;
		private boolean listenerActive;

		private FlixManifestFormPage(FormEditor editor, String id, String title) {
			super(editor, id, title);
			this.syncEditorThrottler = new Throttler(PlatformUI.getWorkbench().getDisplay(), Duration.ofMillis(500), this::syncToEditor);
			this.listenerActive = true;
		}

		@Override
		public void createPartControl(Composite parent) {
			System.out.println("FlixManifestFormEditor.FlixManifestFormPage.createPartControl()");
			super.createPartControl(parent);

			final GridLayout layout = new GridLayout(2, false);
			getManagedForm().getForm().getBody().setLayout(layout);
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {

			final FormToolkit toolkit = managedForm.getToolkit();
			final ScrolledForm form = managedForm.getForm();

			toolkit.createLabel(form.getBody(), "Name:");
			this.nameText = toolkit.createText(form.getBody(), "");
			this.nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			this.nameText.addModifyListener(this::onModify);

			toolkit.createLabel(form.getBody(), "Description:");
			this.descriptionText = toolkit.createText(form.getBody(), "");
			this.descriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			this.descriptionText.addModifyListener(this::onModify);

			toolkit.createLabel(form.getBody(), "Project version:");
			this.projectVersion = toolkit.createText(form.getBody(), "");
			this.projectVersion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			this.projectVersion.addModifyListener(this::onModify);

			toolkit.createLabel(form.getBody(), "Flix version:");
			this.flixVersion = toolkit.createText(form.getBody(), "");
			this.flixVersion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			this.flixVersion.addModifyListener(this::onModify);

			final GridData tableGridData = new GridData(GridData.FILL_HORIZONTAL);
			tableGridData.horizontalSpan = 2;
			tableGridData.heightHint = 200;

			toolkit.createLabel(form.getBody(), "Dependencies:");
			toolkit.createLabel(form.getBody(), "");
			this.dependenciesTableViewer = new DependencyTableViewer(form.getBody());
			this.dependenciesTableViewer.getTable().setLayoutData(tableGridData);

			toolkit.createLabel(form.getBody(), "Maven dependencies:");
			toolkit.createLabel(form.getBody(), "");
			this.mavenDependenciesTableViewer = new MavenDependencyTableViewer(form.getBody());
			this.mavenDependenciesTableViewer.getTable().setLayoutData(tableGridData);
		}

		private void onModify(ModifyEvent event) {
			if (this.listenerActive) {
				FlixManifestFormPage.this.syncEditorThrottler.throttledExec();
			}
		}

		public void syncToEditor() {
			final IDocument document = getDocument();
			final String beforeToml = document.get();
			final MutableFlixManifestToml mutableToml = MutableFlixManifestToml.open(beforeToml);
			mutableToml.setValue(new String[] { "package", "name" }, this.nameText.getText());
			mutableToml.setValue(new String[] { "package", "description" }, this.descriptionText.getText());
			mutableToml.setValue(new String[] { "package", "version" }, this.projectVersion.getText());
			mutableToml.setValue(new String[] { "package", "flix" }, this.flixVersion.getText());
			final List<FlixDependency> flixDependencies = (List<FlixDependency>) this.dependenciesTableViewer.getInput();
			for (final FlixDependency dependency : flixDependencies) {
				mutableToml.setValue(new String[] { "dependencies", dependency.getKey() }, dependency.getVersion());
			}
			final List<MavenDependency> mavenDependencies = (List<MavenDependency>) this.mavenDependenciesTableViewer.getInput();
			for (final MavenDependency dependency : mavenDependencies) {
				mutableToml.setValue(new String[] { "mvn-dependencies", dependency.getKey() }, dependency.getVersion());
			}
			final String afterToml = mutableToml.getContent();
			if (!Objects.equals(beforeToml, afterToml)) {
				document.set(afterToml);
			}
		}

		public void syncFromEditor() {
			final IDocument document = getDocument();
			final FlixManifestToml toml = FlixManifestToml.load(document.get()).orElseThrow();
			this.listenerActive = false;
			try {
				this.nameText.setText(toml.getName());
				this.descriptionText.setText(toml.getDescription());
				this.flixVersion.setText(toml.getFlixVersion().getKey());
				this.projectVersion.setText(toml.getProjectVersion());
				this.dependenciesTableViewer.setInput(toml.getFlixDependencies());
				this.mavenDependenciesTableViewer.setInput(toml.getMavenDependencies());
			} finally {
				this.listenerActive = true;
			}
		}

		private IDocument getDocument() {
			final ITextEditor textEditor = FlixManifestFormEditor.this.editor;
			final IDocumentProvider provider = textEditor.getDocumentProvider();
			final IEditorInput input = FlixManifestFormEditor.this.editor.getEditorInput();
			return provider.getDocument(input);
		}
	}

	private static class DependencyTableViewer extends TableViewer {
		public DependencyTableViewer(Composite parent) {
			super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
			this.setContentProvider(ArrayContentProvider.getInstance());

			final TableViewerColumn typeViewerColumn = new TableViewerColumn(this, SWT.NONE);
			typeViewerColumn.getColumn().setWidth(100);
			typeViewerColumn.getColumn().setText("Type");
			typeViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((FlixDependency) element).getType()));

			final TableViewerColumn pathViewerColumn = new TableViewerColumn(this, SWT.NONE);
			pathViewerColumn.getColumn().setWidth(500);
			pathViewerColumn.getColumn().setText("Path");
			pathViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((FlixDependency) element).getPath()));

			final TableViewerColumn versionViewerColumn = new TableViewerColumn(this, SWT.NONE);
			versionViewerColumn.getColumn().setWidth(100);
			versionViewerColumn.getColumn().setText("Version");
			versionViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((FlixDependency) element).getVersion()));

			final Table table = this.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
		}
	}

	private static class MavenDependencyTableViewer extends TableViewer {
		public MavenDependencyTableViewer(Composite parent) {
			super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
			this.setContentProvider(ArrayContentProvider.getInstance());

			final TableViewerColumn typeViewerColumn = new TableViewerColumn(this, SWT.NONE);
			typeViewerColumn.getColumn().setWidth(300);
			typeViewerColumn.getColumn().setText("Group Id");
			typeViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((MavenDependency) element).getGroupId()));

			final TableViewerColumn pathViewerColumn = new TableViewerColumn(this, SWT.NONE);
			pathViewerColumn.getColumn().setWidth(300);
			pathViewerColumn.getColumn().setText("Artifact Id");
			pathViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((MavenDependency) element).getArtifactId()));

			final TableViewerColumn versionViewerColumn = new TableViewerColumn(this, SWT.NONE);
			versionViewerColumn.getColumn().setWidth(300);
			versionViewerColumn.getColumn().setText("Version");
			versionViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((MavenDependency) element).getVersion()));

			final Table table = this.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
		}
	}
}
