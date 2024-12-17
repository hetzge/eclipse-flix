package de.hetzge.eclipse.flix.navigator;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jface.viewers.ViewerComparator;

import de.hetzge.eclipse.flix.compiler.FlixCompilerProject;

public class FlixViewerComparator extends ViewerComparator {

	public FlixViewerComparator() {
		this(Comparator.naturalOrder());
	}

	public FlixViewerComparator(Comparator<? super String> comparator) {
		super((a, b) -> {
			if (Objects.equals(a, FlixCompilerProject.FLIX_COMPILER_PROJECT_NAME)) {
				return 1;
			} else if (Objects.equals(b, FlixCompilerProject.FLIX_COMPILER_PROJECT_NAME)) {
				return -1;
			} else {
				return comparator.compare(a.toLowerCase(), b.toLowerCase());
			}
		});
	}
}
