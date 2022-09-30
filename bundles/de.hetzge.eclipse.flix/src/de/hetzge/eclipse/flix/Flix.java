package de.hetzge.eclipse.flix;

import org.lxtk.CommandService;
import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceService;
import org.lxtk.lx4e.EclipseCommandService;
import org.lxtk.lx4e.EclipseDocumentService;
import org.lxtk.lx4e.EclipseLanguageService;
import org.lxtk.lx4e.EclipseWorkspaceService;
import org.lxtk.lx4e.refactoring.FileOperationParticipantSupport;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

import de.hetzge.eclipse.flix.model.api.FlixModelManager;
import de.hetzge.eclipse.flix.model.api.IFlixModel;
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
	private final ResourceMonitor postResourceMonitor;
	private final FlixModelManager modelManager;
	private final FlixLanguageToolingManager languageToolingManager;
	private final CommandService commandService;

	Flix() {
		this.documentService = new EclipseDocumentService();
		this.languageService = new EclipseLanguageService();
		this.workspaceService = new EclipseWorkspaceService();
		this.changeFactory = new WorkspaceEditChangeFactory(this.documentService);
		this.fileOperationParticipantSupport = new FileOperationParticipantSupport(this.changeFactory);
		this.changeFactory.setFileOperationParticipantSupport(this.fileOperationParticipantSupport);
		this.documentProvider = new FlixDocumentProvider();
		this.postResourceMonitor = new ResourceMonitor();
		this.modelManager = FlixModelManager.create();
		this.languageToolingManager = new FlixLanguageToolingManager();
		this.commandService = new EclipseCommandService();
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

	public ResourceMonitor getPostResourceMonitor() {
		return this.postResourceMonitor;
	}

	public FlixModelManager getModelManager() {
		return this.modelManager;
	}

	public IFlixModel getModel() {
		return this.modelManager.getModel();
	}

	public FlixLanguageToolingManager getLanguageToolingManager() {
		return this.languageToolingManager;
	}

	public CommandService getCommandService() {
		return this.commandService;
	}

	@Override
	public void close() {
		this.languageToolingManager.close();
	}
}
