
package de.hetzge.eclipse.flix;

import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceService;
import org.lxtk.lx4e.EclipseDocumentService;
import org.lxtk.lx4e.EclipseLanguageService;
import org.lxtk.lx4e.EclipseWorkspaceService;
import org.lxtk.lx4e.refactoring.FileOperationParticipantSupport;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

/**
 * Facade to Flix services.
 */
public class FlixCore {
	/**
	 * Flix document service.
	 */
	public static final DocumentService DOCUMENT_SERVICE = new EclipseDocumentService();

	/**
	 * Flix language service.
	 */
	public static final LanguageService LANGUAGE_SERVICE = new EclipseLanguageService();

	/**
	 * Flix language identifier.
	 */
	public static final String LANGUAGE_ID = "flix"; //$NON-NLS-1$

	/**
	 * Flix workspace service.
	 */
	public static final WorkspaceService WORKSPACE_SERVICE = new EclipseWorkspaceService();

	public static final WorkspaceEditChangeFactory CHANGE_FACTORY = new WorkspaceEditChangeFactory(DOCUMENT_SERVICE);
	public static final WorkspaceEditChangeFactory WORKSPACE_EDIT_CHANGE_FACTORY = new WorkspaceEditChangeFactory(DOCUMENT_SERVICE);
	public static final FileOperationParticipantSupport FILE_OPERATION_PARTICIPANT_SUPPORT = new FileOperationParticipantSupport(WORKSPACE_EDIT_CHANGE_FACTORY);
	static {
		CHANGE_FACTORY.setFileOperationParticipantSupport(FILE_OPERATION_PARTICIPANT_SUPPORT);
	}

	private FlixCore() {
	}
}
