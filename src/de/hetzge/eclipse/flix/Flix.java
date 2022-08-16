package de.hetzge.eclipse.flix;

import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceService;
import org.lxtk.lx4e.EclipseDocumentService;
import org.lxtk.lx4e.EclipseLanguageService;
import org.lxtk.lx4e.EclipseWorkspaceService;
import org.lxtk.lx4e.refactoring.FileOperationParticipantSupport;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

import de.hetzge.eclipse.flix.model.FlixModelManager;
import de.hetzge.eclipse.flix.utils.ResourceMonitor;

public final class Flix implements AutoCloseable {

	public static Flix get() {
		return FlixActivator.getDefault().getFlix();
	}

	private final DocumentService documentService;
	private final LanguageService languageService;
	private final WorkspaceService workspaceService;
	private final WorkspaceEditChangeFactory changeFactory;
	private final FileOperationParticipantSupport fileOperationParticipantSupport;
	private final FlixDocumentProvider documentProvider;
	private final ResourceMonitor resourceMonitor;
	private final FlixModelManager modelManager;
	private final FlixProjectManager projectManager;

	Flix() {
		this.documentService = new EclipseDocumentService();
		this.languageService = new EclipseLanguageService();
		this.workspaceService = new EclipseWorkspaceService();
		this.changeFactory = new WorkspaceEditChangeFactory(this.documentService);
		this.fileOperationParticipantSupport = new FileOperationParticipantSupport(this.changeFactory);
		this.changeFactory.setFileOperationParticipantSupport(this.fileOperationParticipantSupport);
		this.documentProvider = new FlixDocumentProvider();
		this.resourceMonitor = new ResourceMonitor();
		this.modelManager = FlixModelManager.create();
		this.projectManager = new FlixProjectManager();
	}

	public DocumentService getDocumentService() {
		return this.documentService;
	}

	public LanguageService getLanguageService() {
		return this.languageService;
	}

	public WorkspaceService getWorkspaceService() {
		return this.workspaceService;
	}

	public WorkspaceEditChangeFactory getChangeFactory() {
		return this.changeFactory;
	}

	public FileOperationParticipantSupport getFileOperationParticipantSupport() {
		return this.fileOperationParticipantSupport;
	}

	public FlixDocumentProvider getDocumentProvider() {
		return this.documentProvider;
	}

	public ResourceMonitor getResourceMonitor() {
		return this.resourceMonitor;
	}

	public FlixModelManager getModelManager() {
		return this.modelManager;
	}

	public FlixProjectManager getProjectManager() {
		return this.projectManager;
	}

	@Override
	public void close() {
		this.modelManager.close();
		this.projectManager.close();
	}
}
