package de.hetzge.eclipse.flix;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
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
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

// TODO https://github.com/mlutze/fcwg
// TODO LSP Workspace functionality?!
// TODO Document service per project?! -> we have DocumentFilter
// TODO Reregister documents on LSP ready?
// TODO Catch checked exceptions with SafeRunner.run(...)
// TODO embedd flix compiler

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
			 * Register commands ...
			 */
			rollback.add(this.flix.getCommandService().addCommand("flix.runMain", new FlixRunMainCommandHandler())::dispose);
			rollback.add(this.flix.getCommandService().addCommand("flix.cmdRepl", new FlixRunReplCommandHandler())::dispose);

			/*
			 * Init model and projects ...
			 */
			this.flix.getLanguageToolingManager().connectProjects(Flix.get().getModel().getFlixProjects());
			rollback.add(this.flix.getLanguageToolingManager().startMonitor()::dispose);

			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.addResourceChangeListener(this.flix.getPostResourceMonitor(), IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> workspace.removeResourceChangeListener(this.flix.getPostResourceMonitor()));

			rollback.add(this.flix.getPostResourceMonitor().onDidCreateFiles().subscribe(fileCreateEvent -> {
				for (final FileCreate create : fileCreateEvent.getFiles()) {
					final IPath createPath = URIUtil.toPath(create.getUri());
					final IProject project = EclipseUtils.project(createPath).orElseThrow();
					if (project.getFile("flix.jar").getLocation().equals(createPath)) {
						this.flix.getModel().getFlixProject(project).ifPresent(flixProject -> {
							this.flix.getLanguageToolingManager().reconnectProject(flixProject);
						});
					}
				}
			})::dispose);
			rollback.add(this.flix.getPostResourceMonitor().onDidDeleteFiles().subscribe(fileDeleteEvent -> {
				for (final FileDelete delete : fileDeleteEvent.getFiles()) {
					final IPath deletePath = URIUtil.toPath(delete.getUri());
					final IProject project = EclipseUtils.project(deletePath).orElseThrow();
					if (project.getFile("flix.jar").getLocation().equals(deletePath)) {
						final Optional<FlixProject> flixProjectOptional = this.flix.getModel().getFlixProject(project);
						if (flixProjectOptional.isPresent()) {
							final FlixProject flixProject = flixProjectOptional.get();
							this.flix.getLanguageToolingManager().reconnectProject(flixProject);
						}
					}
				}
			})::dispose);
			rollback.add(this.flix.getPostResourceMonitor().onDidChangeFiles().subscribe(fileChangeEvent -> {
				for (final URI uri : fileChangeEvent.getFiles()) {
					final IPath changePath = URIUtil.toPath(uri);
					final IProject project = EclipseUtils.project(changePath).orElseThrow();
					if (project.getFile("flix.jar").getLocation().equals(changePath)) {
						this.flix.getModel().getFlixProject(project).ifPresent(flixProject -> {
							this.flix.getLanguageToolingManager().reconnectProject(flixProject);
						});
					}
					// TODO only on save
//					else if (project.getFile("flix.toml").getLocation().equals(changePath)) {
//						final Optional<FlixProject> flixProjectOptional = this.flix.getModel().getFlixProject(project);
//						if (flixProjectOptional.isPresent()) {
//							final FlixProject flixProject = flixProjectOptional.get();
//							this.flix.getLanguageToolingManager().reconnectProject(flixProject);
//						}
//					}
				}
			})::dispose);
			rollback.add(this.flix.getPostResourceMonitor().onDidOpenProject().subscribe(projectOpenEvent -> {
				final IProject project = projectOpenEvent.getProject();
				final FlixProject flixProject = new FlixProject(project);
				if (project.isOpen()) {
					this.flix.getLanguageToolingManager().reconnectProject(flixProject);
				} else {
					this.flix.getLanguageToolingManager().disconnectProject(flixProject);
				}
			})::dispose);

			this.rollback = rollback;
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
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
