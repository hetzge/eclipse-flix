package de.hetzge.eclipse.flix.model;

import java.net.URI;

import org.eclipse.handly.model.impl.support.IModelManager;
import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.model.impl.LanguageElement;
import org.lxtk.lx4e.model.impl.LanguageSourceFile;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixConstants;

public class FlixSourceFile extends LanguageSourceFile {

	private final FlixModelManager modelManager;
	private final DocumentService documentService;
	private final LanguageService languageService;

	public FlixSourceFile(URI uri) {
		this(null, uri);
	}

	public FlixSourceFile(LanguageElement parent, URI uri) {
		super(parent, uri, FlixConstants.LANGUAGE_ID);
		this.modelManager = Flix.get().getModelManager();
		this.documentService = Flix.get().getDocumentService();
		this.languageService = Flix.get().getLanguageService();
	}

	@Override
	public IModelManager getModelManager_() {
		return this.modelManager;
	}

	@Override
	protected DocumentService getDocumentService() {
		return this.documentService;
	}

	@Override
	protected LanguageService getLanguageService() {
		return this.languageService;
	}
}