package de.hetzge.eclipse.flix;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class IncrementalProjectBuilder1 extends IncrementalProjectBuilder {

	public IncrementalProjectBuilder1() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		System.out.println("IncrementalProjectBuilder1.build()");
		System.out.println(kind);
		return null;
	}

}
