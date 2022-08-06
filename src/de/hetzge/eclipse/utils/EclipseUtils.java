package de.hetzge.eclipse.utils;

import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

public final class EclipseUtils {

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
}
