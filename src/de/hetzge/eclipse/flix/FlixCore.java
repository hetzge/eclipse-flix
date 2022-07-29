
package de.hetzge.eclipse.flix;

import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceService;
import org.lxtk.lx4e.EclipseDocumentService;
import org.lxtk.lx4e.EclipseLanguageService;
import org.lxtk.lx4e.EclipseWorkspaceService;

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

	private FlixCore() {
	}
}
