package de.hetzge.eclipse.flix.utils;

import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.lxtk.FileCreate;
import org.lxtk.FileCreateEvent;
import org.lxtk.FileCreateEventSource;
import org.lxtk.FileDelete;
import org.lxtk.FileDeleteEvent;
import org.lxtk.FileDeleteEventSource;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;

import de.hetzge.eclipse.flix.FlixLogger;

public class ResourceMonitor implements IResourceChangeListener, FileCreateEventSource, FileDeleteEventSource {
	private final EventEmitter<FileCreateEvent> onDidCreateFiles;
	private final EventEmitter<FileDeleteEvent> onDidDeleteFiles;
	private final EventEmitter<FileChangeEvent> onDidChangeFiles;
	private final EventEmitter<ProjectOpenEvent> onDidOpenProject;
	private final EventEmitter<IProject> onDeleteProject;

	public ResourceMonitor() {
		this.onDidCreateFiles = new EventEmitter<>();
		this.onDidDeleteFiles = new EventEmitter<>();
		this.onDidChangeFiles = new EventEmitter<>();
		this.onDidOpenProject = new EventEmitter<>();
		this.onDeleteProject = new EventEmitter<>();
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			handlePostChange(event.getDelta());
		} else if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
			handlePreDelete(event.getResource());
		}
	}

	private void handlePostChange(IResourceDelta delta) {
		final IResource resource = delta.getResource();
		if (resource instanceof IFile) {
			final IFile file = (IFile) resource;
			final int kind = delta.getKind();
			final URI locationURI = file.getLocationURI();
			if (locationURI != null) {
				if (kind == IResourceDelta.ADDED) {
					this.onDidCreateFiles.emit(new FileCreateEvent(List.of(new FileCreate(locationURI))), FlixLogger::logError);
				} else if (kind == IResourceDelta.REMOVED) {
					this.onDidDeleteFiles.emit(new FileDeleteEvent(List.of(new FileDelete(locationURI))), FlixLogger::logError);
				} else if (kind == IResourceDelta.CHANGED) {
					this.onDidChangeFiles.emit(new FileChangeEvent(List.of(locationURI)), FlixLogger::logError);
				}
			} else {
				FlixLogger.logWarning(String.format("Resource delta with null uri for '%s'", file.getName()), null);
			}
		} else if (resource instanceof IProject) {
			final IProject project = (IProject) resource;
			final int flags = delta.getFlags();
			if ((flags & IResourceDelta.OPEN) == IResourceDelta.OPEN) {
				this.onDidOpenProject.emit(new ProjectOpenEvent(project), FlixLogger::logError);
			}
		}
		final IResourceDelta[] affectedChildren = delta.getAffectedChildren();
		for (final IResourceDelta childrenResourceDelta : affectedChildren) {
			handlePostChange(childrenResourceDelta);
		}
	}

	private void handlePreDelete(IResource resource) {
		if (resource instanceof IProject) {
			final IProject project = (IProject) resource;
			this.onDeleteProject.emit(project, FlixLogger::logError);
		}
	}

	@Override
	public EventStream<FileCreateEvent> onDidCreateFiles() {
		return this.onDidCreateFiles;
	}

	@Override
	public EventStream<FileDeleteEvent> onDidDeleteFiles() {
		return this.onDidDeleteFiles;
	}

	public EventEmitter<FileChangeEvent> onDidChangeFiles() {
		return this.onDidChangeFiles;
	}

	public EventEmitter<ProjectOpenEvent> onDidOpenProject() {
		return this.onDidOpenProject;
	}

	public EventEmitter<IProject> onDeleteProject() {
        return this.onDeleteProject;
    }

	public static class ProjectOpenEvent {
		private final IProject project;

		public ProjectOpenEvent(IProject project) {
			this.project = project;
		}

		public IProject getProject() {
			return this.project;
		}
	}
}