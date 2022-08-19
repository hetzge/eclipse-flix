package de.hetzge.eclipse.flix.project;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hetzge.eclipse.utils.EclipseUtils;

public class FlixProjectNatureHandler extends AbstractHandler {

	private static final ILog LOG = Platform.getLog(FlixProjectNatureHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("FlixProjectNatureHandler.execute()");
		final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (!(currentSelection instanceof IStructuredSelection)) {
			LOG.warn("Skip add Flix nature because not a structured selection");
			return Status.OK_STATUS;
		}

		final Object firstElement = ((IStructuredSelection) currentSelection).getFirstElement();
		final IAdapterManager adapterManager = Platform.getAdapterManager();
		final IResource resourceAdapter = adapterManager.getAdapter(firstElement, IResource.class);
		if (resourceAdapter == null) {
			LOG.warn("Skip add Flix nature because no resource found");
			return Status.OK_STATUS;
		}

		final IResource resource = resourceAdapter;
		final IProject project = resource.getProject();
		try {
			final IStatus status = EclipseUtils.addNature(project, FlixProjectNature.ID);
			if (status != Status.OK_STATUS) {
				LOG.warn("Failed to add Flix nature");
			}
			return status;
		} catch (final CoreException exception) {
			throw new ExecutionException(exception.getMessage(), exception);
		}
	}
}