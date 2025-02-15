package de.hetzge.eclipse.flix;

import java.util.Map;

import org.eclipse.core.resources.IIncrementalProjectBuilder2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import de.hetzge.eclipse.flix.model.FlixProject;

public class FlixProjectBuilder extends IncrementalProjectBuilder implements IIncrementalProjectBuilder2 {
	private static final ILog LOG = Platform.getLog(FlixProjectBuilder.class);

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		LOG.info("Flix build kind " + kind);
		if (kind == IncrementalProjectBuilder.FULL_BUILD || kind == IncrementalProjectBuilder.CLEAN_BUILD) {
			clean(monitor);
			return null;
		} else if (kind == IncrementalProjectBuilder.AUTO_BUILD || kind == IncrementalProjectBuilder.INCREMENTAL_BUILD) {
			fastBuild(monitor);
			return null;
		} else {
			throw new IllegalStateException("Unsupported build kind: " + kind);
		}
	}

	@Override
	public void clean(Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		LOG.info("Clean Flix project");
		final FlixProject flixProject = Flix.get().getModel().getFlixProjectOrThrowCoreException(getProject());
		final String projectName = flixProject.getProject().getName();
		monitor.subTask(String.format("Full/Clean build (%s)", projectName));
		Flix.get().getLanguageToolingManager().reconnectProject(flixProject);
	}

	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		return getProject();
	}

	private void fastBuild(IProgressMonitor monitor) throws CoreException {
		LOG.info("Fast build Flix project");
		final FlixProject flixProject = Flix.get().getModel().getFlixProjectOrThrowCoreException(getProject());
		final String projectName = flixProject.getProject().getName();
		monitor.subTask(String.format("Auto/Incremental build (%s)", projectName));
		Flix.get().getLanguageToolingManager().compile(flixProject);
	}
}
