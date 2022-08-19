package de.hetzge.eclipse.flix;

import java.util.Objects;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.handly.model.ElementDeltas;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.IElementDelta;
import org.eclipse.handly.model.impl.support.NotificationManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixModelManager;
import de.hetzge.eclipse.flix.model.IFlixProject;

/**
 * The activator class controls the plug-in life cycle
 */
public class FlixActivator extends AbstractUIPlugin implements IElementChangeListener {

	private static FlixActivator plugin;

	public static FlixActivator getDefault() {
		Objects.requireNonNull(plugin, "Flix plugin is not initialized");
		return plugin;
	}

	private Rollback rollback;
	private Flix flix;

	public FlixActivator() {
		this.rollback = null;
		this.flix = null;
	}

	public Flix getFlix() {
		Objects.requireNonNull(this.flix, "Flix plugin is not initialized");
		return this.flix;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		FlixLogger.logInfo("Let's go Flix");

		SafeRun.run(rollback -> {
			this.flix = new Flix();
			rollback.add(() -> {
				this.flix.close();
				this.flix = null;
			});
			final FlixProjectManager projectManager = this.flix.getProjectManager();

			/*
			 * Init model and projects ...
			 */
			final FlixModelManager modelManager = this.flix.getModelManager();
			final FlixModel model = modelManager.getModel();
			model.getFlixProjects().forEach(flixProject -> {
				System.out.println(">>> " + flixProject);
				projectManager.initializeFlixProject(flixProject);
			});

			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.addResourceChangeListener(this.flix.getResourceMonitor(), IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> workspace.removeResourceChangeListener(this.flix.getResourceMonitor()));
			workspace.addResourceChangeListener(modelManager, IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> workspace.removeResourceChangeListener(modelManager));

			final NotificationManager notificationManager = modelManager.getNotificationManager();
			notificationManager.addElementChangeListener(this);
			rollback.add(() -> notificationManager.removeElementChangeListener(this));

			this.rollback = rollback;
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("FlixActivator.stop()");
		try {
			if (this.rollback != null) {
				this.rollback.run();
				this.rollback = null;
			}
			plugin = null;
		} finally {
			super.stop(context);
		}
	}

	@Override
	public void elementChanged(IElementChangeEvent event) {
		System.out.println("FlixActivator.elementChanged()");
		for (final IElementDelta delta : event.getDeltas()) {
			for (final IElementDelta addedChildDelta : ElementDeltas.getAddedChildren(delta)) {
				final IElement element = ElementDeltas.getElement(addedChildDelta);
				if (element instanceof IFlixProject) {
					final IFlixProject flixProject = (IFlixProject) element;
					Flix.get().getProjectManager().initializeFlixProject(flixProject);
				}
			}
			for (final IElementDelta removedChildDelta : ElementDeltas.getRemovedChildren(delta)) {
				final IElement element = ElementDeltas.getElement(removedChildDelta);
				if (element instanceof IFlixProject) {
					final IFlixProject flixProject = (IFlixProject) element;
					Flix.get().getProjectManager().closeProject(flixProject);
				}
			}
		}
	}

}
