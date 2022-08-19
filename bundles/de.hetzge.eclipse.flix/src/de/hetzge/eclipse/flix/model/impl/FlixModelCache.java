package de.hetzge.eclipse.flix.model.impl;

import java.util.HashMap;

import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.support.ElementCache;
import org.eclipse.handly.model.impl.support.IBodyCache;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.ILanguageSymbol;

import de.hetzge.eclipse.flix.model.api.IFlixModel;

public class FlixModelCache implements IBodyCache {
	private static final int DEFAULT_FILE_SIZE = 250;
	private static final int DEFAULT_CHILDREN_SIZE = DEFAULT_FILE_SIZE * 20; // average 20 children per file

	private Object model;
	private final ElementCache fileCache;
	private final HashMap<IElement, Object> symbolCache;

	public FlixModelCache() {
		// set the size of the caches as a function of the maximum amount of memory available
		final double memoryRatio = getMemoryRatio();
		this.fileCache = new ElementCache((int) (DEFAULT_FILE_SIZE * memoryRatio));
		this.symbolCache = new HashMap<>((int) (DEFAULT_CHILDREN_SIZE * memoryRatio));
	}

	@Override
	public Object get(IElement element) {
		if (element instanceof IFlixModel) {
			return this.model;
		} else if (element instanceof ILanguageSourceFile) {
			return this.fileCache.get(element);
		} else {
			return this.symbolCache.get(element);
		}
	}

	@Override
	public Object peek(IElement element) {
		if (element instanceof IFlixModel) {
			return this.model;
		} else if (element instanceof ILanguageSourceFile) {
			return this.fileCache.peek(element);
		} else {
			return this.symbolCache.get(element);
		}
	}

	@Override
	public void put(IElement element, Object body) {
		if (element instanceof IFlixModel) {
			this.model = body;
		} else if (element instanceof ILanguageSourceFile) {
			this.fileCache.put(element, body);
		} else if (element instanceof ILanguageSymbol) {
			this.symbolCache.put(element, body);
		} else {
			System.out.println("Skip put element");
		}
	}

	@Override
	public void remove(IElement element) {
		if (element instanceof IFlixModel) {
			this.model = null;
		} else if (element instanceof ILanguageSourceFile) {
			this.fileCache.remove(element);
		} else {
			this.symbolCache.remove(element);
		}
	}

	private double getMemoryRatio() {
		final long maxMemory = Runtime.getRuntime().maxMemory();
		// if max memory is infinite, set the ratio to 4d
		// which corresponds to the 256MB that Eclipse defaults to
		// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=111299)
		return maxMemory == Long.MAX_VALUE ? 4d : ((double) maxMemory) / (64 * 0x100000); // 64MB is the base memory for most JVM
	}
}
