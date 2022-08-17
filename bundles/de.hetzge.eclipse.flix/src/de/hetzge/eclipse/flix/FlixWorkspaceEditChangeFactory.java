package de.hetzge.eclipse.flix;

import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

/**
 * Flix-specific extension of {@link WorkspaceEditChangeFactory}.
 */
public class FlixWorkspaceEditChangeFactory extends WorkspaceEditChangeFactory {
	public static final FlixWorkspaceEditChangeFactory INSTANCE = new FlixWorkspaceEditChangeFactory();

	private FlixWorkspaceEditChangeFactory() {
		super(Flix.get().getDocumentService());
	}
}
