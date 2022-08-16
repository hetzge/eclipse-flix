package de.hetzge.eclipse.flix;

import java.util.Objects;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.utils.EclipseUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class FlixActivator extends AbstractUIPlugin {

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

		SafeRun.run(rollback -> {
			this.flix = new Flix();
			rollback.add(() -> {
				this.flix.close();
				this.flix = null;
			});

			/*
			 * Init model ...
			 */
			final FlixModel model = this.flix.getModelManager().getModel();
			model.getProjects().forEach(project -> {
				System.out.println(">>> " + project);
			});

			/*
			 * When open a document then check if the flix environment for this project is
			 * already initialized and initialize if necessary.
			 */
			rollback.add(this.flix.getDocumentService().onDidAddTextDocument().subscribe(document -> {
				final FlixProjectManager projectManager = this.flix.getProjectManager();
				EclipseUtils.getProject(document).ifPresent(projectManager::initializeFlixProject);
			})::dispose);

			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.addResourceChangeListener(this.flix.getResourceMonitor(), IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> workspace.removeResourceChangeListener(this.flix.getResourceMonitor()));
			workspace.addResourceChangeListener(this.flix.getModelManager(), IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> workspace.removeResourceChangeListener(this.flix.getModelManager()));

			this.rollback = rollback;
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			System.out.println("Activator.stop()");
			if (this.rollback != null) {
				this.rollback.run();
				this.rollback = null;
			}
			plugin = null;
		} finally {
			super.stop(context);
		}
	}

}
