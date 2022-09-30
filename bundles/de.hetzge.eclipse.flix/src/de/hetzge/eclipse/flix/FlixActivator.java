package de.hetzge.eclipse.flix;

import java.net.URI;
import java.util.Objects;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.model.ElementDeltas;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.model.IElementDelta;
import org.eclipse.handly.model.impl.support.NotificationManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.lxtk.FileCreate;
import org.lxtk.FileDelete;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.flix.launch.FlixRunMainCommandHandler;
import de.hetzge.eclipse.flix.launch.FlixRunReplCommandHandler;
import de.hetzge.eclipse.flix.model.api.FlixModelManager;
import de.hetzge.eclipse.flix.model.api.IFlixModel;
import de.hetzge.eclipse.flix.model.api.IFlixProject;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

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

			Job.create("Initialize flix tooling", monitor -> {

				/*
				 * Register commands ...
				 */
				rollback.add(this.flix.getCommandService().addCommand("flix.runMain", new FlixRunMainCommandHandler())::dispose);
				rollback.add(this.flix.getCommandService().addCommand("flix.cmdRepl", new FlixRunReplCommandHandler())::dispose);

				/*
				 * Init model and projects ...
				 */
				final FlixModelManager modelManager = this.flix.getModelManager();
				final IFlixModel model = modelManager.getModel();
				for (final IFlixProject flixProject : model.getFlixProjects()) {
					System.out.println(">>> " + flixProject);
					this.flix.getLanguageToolingManager().connectProject(flixProject);
				}

				final IWorkspace workspace = ResourcesPlugin.getWorkspace();
				workspace.addResourceChangeListener(this.flix.getPostResourceMonitor(), IResourceChangeEvent.POST_CHANGE);
				rollback.add(() -> workspace.removeResourceChangeListener(this.flix.getPostResourceMonitor()));
				workspace.addResourceChangeListener(modelManager, IResourceChangeEvent.POST_CHANGE);
				rollback.add(() -> workspace.removeResourceChangeListener(modelManager));

				final NotificationManager notificationManager = modelManager.getNotificationManager();
				notificationManager.addElementChangeListener(this);
				rollback.add(() -> notificationManager.removeElementChangeListener(this));

				rollback.add(this.flix.getPostResourceMonitor().onDidCreateFiles().subscribe(fileCreateEvent -> {
					System.out.println("CREATED");
					for (final FileCreate create : fileCreateEvent.getFiles()) {
						final IPath deletePath = URIUtil.toPath(create.getUri());
						final IProject project = EclipseUtils.project(deletePath).orElseThrow();
						if (project.getFile("flix.jar").getLocation().equals(deletePath)) {
							this.flix.getModel().getFlixProject(project).ifPresent(flixProject -> {
								this.flix.getLanguageToolingManager().reconnectProject(flixProject);
							});
						}
					}
				})::dispose);
				rollback.add(this.flix.getPostResourceMonitor().onDidDeleteFiles().subscribe(fileDeleteEvent -> {
					System.out.println("DELETED");
					for (final FileDelete delete : fileDeleteEvent.getFiles()) {
						final IPath deletePath = URIUtil.toPath(delete.getUri());
						final IProject project = EclipseUtils.project(deletePath).orElseThrow();
						if (project.getFile("flix.jar").getLocation().equals(deletePath)) {
							this.flix.getModel().getFlixProject(project).ifPresent(flixProject -> {
								this.flix.getLanguageToolingManager().reconnectProject(flixProject);
							});
						}
					}
				})::dispose);
				rollback.add(this.flix.getPostResourceMonitor().onDidChangeFiles().subscribe(fileChangeEvent -> {
					System.out.println("CHANGED");
					for (final URI uri : fileChangeEvent.getFiles()) {
						final IPath changePath = URIUtil.toPath(uri);
						final IProject project = EclipseUtils.project(changePath).orElseThrow();
						if (project.getFile("flix.jar").getLocation().equals(changePath)) {
							this.flix.getModel().getFlixProject(project).ifPresent(flixProject -> {
								this.flix.getLanguageToolingManager().reconnectProject(flixProject);
							});
						}
					}
				})::dispose);

				this.rollback = rollback;
			}).schedule();
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
					Flix.get().getLanguageToolingManager().connectProject(flixProject);
				}
				System.out.println("added " + element.getClass());
			}
			for (final IElementDelta removedChildDelta : ElementDeltas.getRemovedChildren(delta)) {
				final IElement element = ElementDeltas.getElement(removedChildDelta);
				if (element instanceof IFlixProject) {
					final IFlixProject flixProject = (IFlixProject) element;
					Flix.get().getLanguageToolingManager().disconnectProject(flixProject);
				}
				System.out.println("removed " + element.getClass());
			}
			for (final IElementDelta changedChildDelta : ElementDeltas.getChangedChildren(delta)) {
				final IElement element = ElementDeltas.getElement(changedChildDelta);
				System.out.println("changed " + element.getClass());
			}
			for (final IElementDelta affectedChildDelta : ElementDeltas.getAffectedChildren(delta)) {
				final IElement element = ElementDeltas.getElement(affectedChildDelta);
				System.out.println("affected " + element.getClass());
			}
		}
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put(FlixConstants.FLIX_ICON_IMAGE_KEY, imageDescriptorFromPlugin(FlixConstants.PLUGIN_ID, "assets/icons/icon.png"));
		reg.put(FlixConstants.FOLDER_ICON_IMAGE_KEY, ImageDescriptor.createFromURL(Utils.createUrl("platform:/plugin/org.eclipse.ui.ide/icons/full/obj16/folder.png")));
	}

	public static Image getImage(String symbolicName) {
		return FlixActivator.getDefault().getImageRegistry().get(symbolicName);
	}

	public static ImageDescriptor getImageDescriptor(String symbolicName) {
		return FlixActivator.getDefault().getImageRegistry().getDescriptor(symbolicName);
	}
}
