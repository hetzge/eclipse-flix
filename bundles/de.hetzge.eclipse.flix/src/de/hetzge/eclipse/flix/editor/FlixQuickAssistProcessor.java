package de.hetzge.eclipse.flix.editor;

import java.util.Objects;
import java.util.function.Supplier;

import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.codeaction.AbstractQuickAssistProcessor;

import de.hetzge.eclipse.flix.Flix;


/**
 * Flix-specific extension of {@link AbstractQuickAssistProcessor}.
 */
public class FlixQuickAssistProcessor extends AbstractQuickAssistProcessor {
	private final Supplier<LanguageOperationTarget> targetSupplier;

	public FlixQuickAssistProcessor(Supplier<LanguageOperationTarget> targetSupplier) {
		this.targetSupplier = Objects.requireNonNull(targetSupplier);
	}

	@Override
	protected LanguageOperationTarget getLanguageOperationTarget() {
		return this.targetSupplier.get();
	}

	@Override
	protected IWorkspaceEditChangeFactory getWorkspaceEditChangeFactory() {
		return Flix.get().getChangeFactory();
	}
}
