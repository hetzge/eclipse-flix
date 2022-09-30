package de.hetzge.eclipse.flix.model.impl;

import java.net.URI;

import org.eclipse.handly.model.impl.support.IModelManager;
import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.model.impl.LanguageElement;
import org.lxtk.lx4e.model.impl.LanguageSourceFile;

import de.hetzge.eclipse.flix.Flix;
import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.api.IFlixSourceFile;

public class FlixSourceFile extends LanguageSourceFile implements IFlixSourceFile {

	public FlixSourceFile(URI uri) {
		this(null, uri);
	}

	public FlixSourceFile(LanguageElement parent, URI uri) {
		super(parent, uri, FlixConstants.LANGUAGE_ID);
	}

	@Override
	public IModelManager getModelManager_() {
		return Flix.get().getModelManager();
	}

	@Override
	protected DocumentService getDocumentService() {
		return Flix.get().getDocumentService();
	}

	@Override
	protected LanguageService getLanguageService() {
		return Flix.get().getLanguageService();
	}
}