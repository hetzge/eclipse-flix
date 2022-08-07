package de.hetzge.eclipse.flix;

import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
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

public class ResourceMonitor implements IResourceChangeListener, FileCreateEventSource, FileDeleteEventSource {
	private final EventEmitter<FileCreateEvent> onDidCreateFiles;
	private final EventEmitter<FileDeleteEvent> onDidDeleteFiles;

	public ResourceMonitor() {
		this.onDidCreateFiles = new EventEmitter<>();
		this.onDidDeleteFiles = new EventEmitter<>();
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		handle(event.getDelta());
	}

	private void handle(IResourceDelta delta) {
		final IResource resource = delta.getResource();
		if (resource instanceof final IFile file) {
			final int kind = delta.getKind();
			final URI locationURI = file.getLocationURI();
			if (kind == IResourceDelta.ADDED) {
				this.onDidCreateFiles.emit(new FileCreateEvent(List.of(new FileCreate(locationURI))), FlixLogger::logError);
			} else if (kind == IResourceDelta.REMOVED) {
				this.onDidDeleteFiles.emit(new FileDeleteEvent(List.of(new FileDelete(locationURI))), FlixLogger::logError);
			}
		}
		final IResourceDelta[] affectedChildren = delta.getAffectedChildren();
		for (final IResourceDelta childrenResourceDelta : affectedChildren) {
			handle(childrenResourceDelta);
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
}