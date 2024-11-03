package de.hetzge.eclipse.flix.manifest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.Throttler;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
		if (this.formPage.getPartControl() != null) {
			this.formPage.syncToEditor();
		}
		this.editor.doSave(monitor);
	}

	@Override
	public void doSaveAs() {
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
		private final List<FlixDependency> dependencies;
		private final List<MavenDependency> mavenDependencies;

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
			this.dependencies = new ArrayList<>();
			this.mavenDependencies = new ArrayList<>();
		}

		@Override
		public void createPartControl(Composite parent) {
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

			toolkit.createLabel(form.getBody(), "Dependencies:");
			toolkit.createLabel(form.getBody(), "");

			final Composite dependenciesComposite = toolkit.createComposite(form.getBody());
			dependenciesComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).hint(SWT.DEFAULT, 200).create());
			dependenciesComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());

			this.dependenciesTableViewer = new DependencyTableViewer(dependenciesComposite);
			this.dependenciesTableViewer.getTable().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 200).create());
			this.dependenciesTableViewer.setInput(this.dependencies);

			final SelectionListener addDependencySelectionListener = SelectionListener.widgetSelectedAdapter(event -> {
				final EditDependencyDialog dialog = new EditDependencyDialog(Display.getDefault().getActiveShell(), null);
				if (dialog.open() == Dialog.OK) {
					this.dependencies.add(dialog.getResult());
					this.dependenciesTableViewer.refresh();
				}
			});
			final SelectionListener removeDependencySelectionListener = SelectionListener.widgetSelectedAdapter(event -> {
				final ISelection selection = this.dependenciesTableViewer.getSelection();
				final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				final List<FlixDependency> selectedDependencies = structuredSelection.stream().map(FlixDependency.class::cast).toList();
				this.dependencies.removeAll(selectedDependencies);
				this.dependenciesTableViewer.refresh();
			});
			createDependencyActions(dependenciesComposite, toolkit, this.dependenciesTableViewer, addDependencySelectionListener, removeDependencySelectionListener);

			toolkit.createLabel(form.getBody(), "Maven dependencies:");
			toolkit.createLabel(form.getBody(), "");

			final Composite mavenDependenciesComposite = toolkit.createComposite(form.getBody());
			mavenDependenciesComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).hint(SWT.DEFAULT, 200).create());
			mavenDependenciesComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());

			this.mavenDependenciesTableViewer = new MavenDependencyTableViewer(mavenDependenciesComposite);
			this.mavenDependenciesTableViewer.getTable().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 200).create());
			this.mavenDependenciesTableViewer.setInput(this.mavenDependencies);

			final SelectionListener addMavenDependencySelectionListener = SelectionListener.widgetSelectedAdapter(event -> {
				final EditMavenDependencyDialog dialog = new EditMavenDependencyDialog(Display.getDefault().getActiveShell(), null);
				if (dialog.open() == Dialog.OK) {
					this.mavenDependencies.add(dialog.getResult());
					this.mavenDependenciesTableViewer.refresh();
				}
			});
			final SelectionListener removeMavenDependencySelectionListener = SelectionListener.widgetSelectedAdapter(event -> {
				final ISelection selection = this.mavenDependenciesTableViewer.getSelection();
				final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				final List<MavenDependency> selectedDependencies = structuredSelection.stream().map(MavenDependency.class::cast).toList();
				this.mavenDependencies.removeAll(selectedDependencies);
				this.mavenDependenciesTableViewer.refresh();
			});
			createDependencyActions(mavenDependenciesComposite, toolkit, this.mavenDependenciesTableViewer, addMavenDependencySelectionListener, removeMavenDependencySelectionListener);

		}

		public void createDependencyActions(Composite parent, FormToolkit toolkit, TableViewer tableViewer, SelectionListener onAdd, SelectionListener onRemove) {
			final Composite dependenciesButtonsComposite = toolkit.createComposite(parent);
			dependenciesButtonsComposite.setLayoutData(GridDataFactory.fillDefaults().hint(200, SWT.DEFAULT).create());
			dependenciesButtonsComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
			final Button addDependencyButton = toolkit.createButton(dependenciesButtonsComposite, "Add", SWT.NONE);
			addDependencyButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			addDependencyButton.addSelectionListener(onAdd);
			final Button removeSelectedDependenciesButton = toolkit.createButton(dependenciesButtonsComposite, "Remove selected", SWT.NONE);
			removeSelectedDependenciesButton.setEnabled(false);
			removeSelectedDependenciesButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			removeSelectedDependenciesButton.addSelectionListener(onRemove);
			tableViewer.addSelectionChangedListener(event -> {
				removeSelectedDependenciesButton.setEnabled(!event.getSelection().isEmpty());
			});
		}

		private void onModify(ModifyEvent event) {
			if (this.listenerActive) {
				FlixManifestFormPage.this.syncEditorThrottler.throttledExec();
			}
		}

		public void syncToEditor() {
			final IDocument document = getDocument();
			final String beforeToml = document.get();
			final FlixManifestToml beforeManifestToml = FlixManifestToml.load(beforeToml).orElseThrow();
			final MutableFlixManifestToml mutableToml = MutableFlixManifestToml.open(beforeToml);
			mutableToml.setValue(new String[] { "package", "name" }, this.nameText.getText());
			mutableToml.setValue(new String[] { "package", "description" }, this.descriptionText.getText());
			mutableToml.setValue(new String[] { "package", "version" }, this.projectVersion.getText());
			mutableToml.setValue(new String[] { "package", "flix" }, this.flixVersion.getText());

			final ArrayList<FlixDependency> toRemoveFlixDependencies = new ArrayList<>(beforeManifestToml.getFlixDependencies());
			toRemoveFlixDependencies.removeAll(this.dependencies);
			for (final FlixDependency dependency : toRemoveFlixDependencies) {
				mutableToml.unsetValue(new String[] { "dependencies", dependency.getKey() });
			}
			for (final FlixDependency dependency : this.dependencies) {
				mutableToml.setValue(new String[] { "dependencies", dependency.getKey() }, dependency.getVersion());
			}

			final ArrayList<MavenDependency> toRemoveMavenDependencies = new ArrayList<>(beforeManifestToml.getMavenDependencies());
			toRemoveMavenDependencies.removeAll(this.mavenDependencies);
			for (final MavenDependency dependency : toRemoveMavenDependencies) {
				mutableToml.unsetValue(new String[] { "mvn-dependencies", dependency.getKey() });
			}
			for (final MavenDependency dependency : this.mavenDependencies) {
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
				this.dependencies.clear();
				this.dependencies.addAll(toml.getFlixDependencies());
				this.dependenciesTableViewer.refresh();
				this.mavenDependencies.clear();
				this.mavenDependencies.addAll(toml.getMavenDependencies());
				this.mavenDependenciesTableViewer.refresh();
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
			typeViewerColumn.getColumn().setWidth(250);
			typeViewerColumn.getColumn().setText("Group Id");
			typeViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((MavenDependency) element).getGroupId()));

			final TableViewerColumn pathViewerColumn = new TableViewerColumn(this, SWT.NONE);
			pathViewerColumn.getColumn().setWidth(250);
			pathViewerColumn.getColumn().setText("Artifact Id");
			pathViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((MavenDependency) element).getArtifactId()));

			final TableViewerColumn versionViewerColumn = new TableViewerColumn(this, SWT.NONE);
			versionViewerColumn.getColumn().setWidth(200);
			versionViewerColumn.getColumn().setText("Version");
			versionViewerColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(element -> ((MavenDependency) element).getVersion()));

			final Table table = this.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
		}
	}

	private static class EditDependencyDialog extends Dialog {
		private Text typeText;
		private Text pathText;
		private Text versionText;
		private final FlixDependency input;
		private FlixDependency result;

		public EditDependencyDialog(Shell parentShell, FlixDependency input) {
			super(parentShell);
			this.input = input;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 300);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(this.input == null ? "Add Dependency" : "Edit Dependency");
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			final Composite area = (Composite) super.createDialogArea(parent);
			new Label(area, SWT.NONE).setText("Type:");
			this.typeText = new Text(area, SWT.SINGLE | SWT.BORDER);
			this.typeText.setText(this.input != null ? this.input.getType() : "github");
			this.typeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new Label(area, SWT.NONE).setText("Path:");
			this.pathText = new Text(area, SWT.SINGLE | SWT.BORDER);
			this.pathText.setText(this.input != null ? this.input.getPath() : "");
			this.pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new Label(area, SWT.NONE).setText("Version:");
			this.versionText = new Text(area, SWT.SINGLE | SWT.BORDER);
			this.versionText.setText(this.input != null ? this.input.getVersion() : "");
			this.versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			return area;
		}

		@Override
		protected void okPressed() {
			this.result = new FlixDependency(
					this.typeText.getText(),
					this.pathText.getText(),
					this.versionText.getText());
			super.okPressed();
		}

		public FlixDependency getResult() {
			return this.result;
		}
	}

	private static class EditMavenDependencyDialog extends Dialog {
		private Text groupIdText;
		private Text artifactIdText;
		private Text versionText;
		private final boolean isAdd;
		private final MavenDependency input;
		private MavenDependency result;

		public EditMavenDependencyDialog(Shell parentShell, MavenDependency input) {
			super(parentShell);
			this.isAdd = input == null;
			this.input = Optional.ofNullable(input).or(EditMavenDependencyDialog::parseFromClipboard).orElse(null);
		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 300);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(this.isAdd ? "Add Maven Dependency" : "Edit Maven Dependency");
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			final Composite area = (Composite) super.createDialogArea(parent);
			new Label(area, SWT.NONE).setText("Group Id:");
			this.groupIdText = new Text(area, SWT.SINGLE | SWT.BORDER);
			this.groupIdText.setText(this.input != null ? this.input.getGroupId() : "");
			this.groupIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new Label(area, SWT.NONE).setText("Artifact Id:");
			this.artifactIdText = new Text(area, SWT.SINGLE | SWT.BORDER);
			this.artifactIdText.setText(this.input != null ? this.input.getArtifactId() : "");
			this.artifactIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			new Label(area, SWT.NONE).setText("Version:");
			this.versionText = new Text(area, SWT.SINGLE | SWT.BORDER);
			this.versionText.setText(this.input != null ? this.input.getVersion() : "");
			this.versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			return area;
		}

		@Override
		protected void okPressed() {
			this.result = new MavenDependency(
					this.groupIdText.getText(),
					this.artifactIdText.getText(),
					this.versionText.getText());
			super.okPressed();
		}

		public MavenDependency getResult() {
			return this.result;
		}

		private static Optional<MavenDependency> parseFromClipboard() {
			final Clipboard clipboard = new Clipboard(Display.getCurrent());
			final String contents = (String) clipboard.getContents(TextTransfer.getInstance());
			if (contents == null || contents.isEmpty()) {
				return Optional.empty();
			}
			final boolean isXml = contents.trim().startsWith("<");
			if (isXml) {
				try {
					final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
					final NodeList dependencyNodes = document.getElementsByTagName("dependency");
					for (int i = 0; i < dependencyNodes.getLength(); i++) {
						final Node dependencyNode = dependencyNodes.item(i);
						final String groupId = getDirectChild(dependencyNode, "groupId").map(Element::getTextContent).orElse("");
						final String artifactId = getDirectChild(dependencyNode, "artifactId").map(Element::getTextContent).orElse("");
						final String version = getDirectChild(dependencyNode, "version").map(Element::getTextContent).orElse("");
						return Optional.of(new MavenDependency(groupId, artifactId, version));
					}
				} catch (SAXException | IOException | ParserConfigurationException exception) {
					exception.printStackTrace(); // TODO logging
					return Optional.empty();
				}
			}
			return Optional.empty();
		}

		private static Optional<Element> getDirectChild(Node parent, String name) {
			for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child instanceof Element && name.equals(child.getNodeName())) {
					return Optional.of((Element) child);
				}
			}
			return Optional.empty();
		}
	}
}
