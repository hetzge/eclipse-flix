package de.hetzge.eclipse.flix.editor.outline;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.DocumentSymbol;

public final class Outline {
	private final List<DocumentSymbol> rootSymbols;

	public Outline(List<DocumentSymbol> rootSymbols) {
		this.rootSymbols = rootSymbols.stream().sorted(Comparator.comparingInt(it -> it.getRange().getStart().getLine())).collect(Collectors.toList());
	}

	public List<DocumentSymbol> getRootSymbols() {
		return this.rootSymbols;
	}

	public void visitPaths(Consumer<LinkedList<DocumentSymbol>> consumer) {
		for (final DocumentSymbol documentSymbol : this.rootSymbols) {
			visitPaths(List.of(documentSymbol), consumer);
		}
	}

	private void visitPaths(List<DocumentSymbol> parents, Consumer<LinkedList<DocumentSymbol>> consumer) {
		if (parents.isEmpty()) {
			return;
		}
		consumer.accept(new LinkedList<DocumentSymbol>(parents));
		for (final DocumentSymbol child : parents.get(parents.size() - 1).getChildren()) {
			final LinkedList<DocumentSymbol> newParents = new LinkedList<>();
			newParents.addAll(parents);
			newParents.add(child);
			visitPaths(newParents, consumer);
		}
	}
}