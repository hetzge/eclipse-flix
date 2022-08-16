package de.hetzge.eclipse.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.TextDocument;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.util.ResourceUtil;
import org.osgi.framework.Bundle;

import de.hetzge.eclipse.flix.Activator;

public final class EclipseUtils {

	private static final ILog LOG = Platform.getLog(EclipseUtils.class);

	private EclipseUtils() {
	}

	public static void visitFiles(IContainer container, Consumer<IFile> fileConsumer) {
		try {
			for (final IResource member : container.members()) {
				if (member instanceof IContainer) {
					visitFiles((IContainer) member, fileConsumer);
				} else if (member instanceof IFile) {
					fileConsumer.accept((IFile) member);
				}
			}
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static Optional<IFile> getFile(IEditorInput input) {
		return Optional.ofNullable(input.getAdapter(IFile.class));
	}

	public static Optional<IFile> getFile(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
			if (firstElement instanceof IFile) {
				return Optional.of((IFile) firstElement);
			}
		}
		return Optional.empty();
	}

	public static Optional<IFile> getFile(IEditorPart editor) {
		final IEditorInput editorInput = editor.getEditorInput();
		if (editorInput instanceof IFileEditorInput) {
			return Optional.of(((IFileEditorInput) editorInput).getFile());
		}
		return Optional.empty();
	}

	public static IProject[] projects() {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceRoot root = workspace.getRoot();
		return root.getProjects();
	}

	public static Optional<IProject> project(String name) {
		for (final IProject project : projects()) {
			if (project.getName().equals(name)) {
				return Optional.of(project);
			}
		}
		return Optional.empty();
	}

	public static Optional<IProject> project(IPath pathInProject) {
		for (final IProject project : projects()) {
			if (project.getLocation().isPrefixOf(pathInProject)) {
				return Optional.of(project);
			}
		}
		return Optional.empty();
	}

	public static IProject defaultProject() {
		final IProject[] projects = projects();
		if (projects.length > 0) {
			return projects[0];
		} else {
			throw new IllegalStateException("No projects in workspace");
		}
	}

	public static Optional<IProject> getProject(TextDocument document) {
		if (document instanceof EclipseTextDocument) {
			final IFile file = ResourceUtil.getFile(((EclipseTextDocument) document).getCorrespondingElement());
			if (file != null) {
				return Optional.of(file.getProject());
			}
		}
		return Optional.empty();
	}

	public static MessageConsole console(String name) {
		final ConsolePlugin plugin = ConsolePlugin.getDefault();
		final IConsoleManager consoleManager = plugin.getConsoleManager();
		final IConsole[] existing = consoleManager.getConsoles();
		for (final IConsole console : existing) {
			if (name.equals(console.getName())) {
				return (MessageConsole) console;
			}
		}

		// no console found, so create a new one
		final MessageConsole newConsole = new MessageConsole(name, null);
		consoleManager.addConsoles(new IConsole[] { newConsole });
		return newConsole;
	}

	/**
	 * Only works in ui thread.
	 */
	public static Optional<IEditorPart> activeEditor() {
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		final IWorkbenchPage activePage = window == null ? null : window.getActivePage();
		return Optional.ofNullable(activePage == null ? activeEditor0() : activePage.getActiveEditor());
	}

	private static IEditorPart activeEditor0() {
		for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			final IWorkbenchPage activePage = window.getActivePage();
			if (activePage != null) {
				final IEditorPart activeEditor = activePage.getActiveEditor();
				if (activeEditor != null) {
					return activeEditor;
				}
			}
		}
		return null;
	}

	public static Optional<ITextEditor> activeTextEditor() {
		return activeEditor().filter(ITextEditor.class::isInstance).map(ITextEditor.class::cast);
	}

	public static Optional<IFileEditorInput> activeFileEditorInput() {
		return activeEditor().map(IEditorPart::getEditorInput).filter(IFileEditorInput.class::isInstance).map(IFileEditorInput.class::cast);
	}

	public static Optional<IFile> activeFile() {
		return activeFileEditorInput().map(IFileEditorInput::getFile);
	}

	public static Optional<IResource> activeResource() {
		return activeFile().flatMap(file -> Optional.ofNullable(file.getAdapter(IResource.class)));
	}

	public static Optional<IDocument> activeDocument() {
		return activeTextEditor().map(editor -> editor.getDocumentProvider().getDocument(editor.getEditorInput()));
	}

	public static Optional<IPath> path(EditorPart editor) {
		final IEditorInput editorInput = editor.getEditorInput();

		if (editorInput instanceof FileEditorInput) {
			final FileEditorInput fileEditorInput = (FileEditorInput) editorInput;
			return Optional.of(fileEditorInput.getPath());
		} else {
			return Optional.empty();
		}
	}

	public static Image createImage(String path) {
		final Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		return ImageDescriptor.createFromURL(bundle.getEntry(path)).createImage();
	}

	public static IPath createResourcePath(String path) throws IOException {
		try {
			final URL internalUrl = createInternalUrl(path);
			final URL url = FileLocator.toFileURL(internalUrl);
			return Path.fromOSString(URIUtil.toFile(URIUtil.toURI(url)).getAbsolutePath());
		} catch (final URISyntaxException exception) {
			throw new IllegalStateException(exception);
		}
	}

	public static URL createInternalUrl(String path) {
		final Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		return FileLocator.find(bundle, new Path(path), null);
	}

	public static void openEditorOutsideProject(IPath path) {
		final IFileStore fileStore = EFS.getLocalFileSystem().getStore(Path.fromPortableString("/")).getFileStore(path);

		if (fileStore.fetchInfo().isDirectory()) {
			LOG.warn(String.format("Path is a directory '%s'", path.toOSString()));
			return;
		}

		if (!fileStore.fetchInfo().exists()) {
			LOG.warn(String.format("Path does not exist '%s'", path.toOSString()));
			return;
		}

		try {
			final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IDE.openEditorOnFileStore(page, fileStore);
		} catch (final PartInitException exception) {
			throw new IllegalStateException(String.format("Failed to open file '%s'", path.toOSString()), exception);
		}
	}

}
