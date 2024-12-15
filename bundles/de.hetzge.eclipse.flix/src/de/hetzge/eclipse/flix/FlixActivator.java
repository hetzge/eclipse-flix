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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.lxtk.FileCreate;
import org.lxtk.FileDelete;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.flix.editor.FlixEditor;
import de.hetzge.eclipse.flix.launch.FlixRunMainCommandHandler;
import de.hetzge.eclipse.flix.launch.FlixRunReplCommandHandler;
import de.hetzge.eclipse.flix.manifest.FlixManifestToml;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

// TODO https://github.com/mlutze/fcwg
// TODO LSP Workspace functionality?!
// TODO Document service per project?! -> we have DocumentFilter
// TODO Reregister documents on LSP ready?
// TODO Catch checked exceptions with SafeRunner.run(...)
// TODO embedd flix compiler
// TODO create flix toml if missing
// TODO use progress monitor in language tooling initialization

// TODO :eval main()
// TODO :eval selection
// TODO :eval chat

// TODO tree icons https://stackoverflow.com/questions/27718357/how-to-change-file-folder-resource-icons-from-existing-eclipse-views

// TODO execute flix command shortcut

// TODO fix reference search (absolute uri)
// TODO refresh library if necessary (flix version)

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

			this.flix.getLanguageToolingManager().connectProjects(Flix.get().getModel().getOrCreateFlixProjects());
			rollback.add(this.flix.getLanguageToolingManager().startMonitor()::dispose);

			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.addResourceChangeListener(this.flix.getPostResourceMonitor(), IResourceChangeEvent.POST_CHANGE);
			rollback.add(() -> workspace.removeResourceChangeListener(this.flix.getPostResourceMonitor()));

			rollback.add(this.flix.getPostResourceMonitor().onDidCreateFiles().subscribe(fileCreateEvent -> {
				for (final FileCreate create : fileCreateEvent.getFiles()) {
					final IPath createPath = URIUtil.toPath(create.getUri());
					final Optional<IProject> projectOptional = EclipseUtils.project(createPath);
					if (projectOptional.isEmpty()) {
						return;
					}
					final IProject project = projectOptional.get();
					if (project.getFile("flix.jar").getLocation().equals(createPath)) {
						this.flix.getModel().getOrCreateFlixProject(project).ifPresent(flixProject -> {
							this.flix.getLanguageToolingManager().reconnectProject(flixProject);
						});
					}
				}
			})::dispose);
			rollback.add(this.flix.getPostResourceMonitor().onDidDeleteFiles().subscribe(fileDeleteEvent -> {
				for (final FileDelete delete : fileDeleteEvent.getFiles()) {
					final IPath deletePath = URIUtil.toPath(delete.getUri());
					final Optional<IProject> projectOptional = EclipseUtils.project(deletePath);
					if (projectOptional.isEmpty()) {
						return;
					}
					final IProject project = projectOptional.get();
					if (project.getFile(FlixProject.FLIX_JAR_FILE_NAME).getLocation().equals(deletePath)) {
						final Optional<FlixProject> flixProjectOptional = this.flix.getModel().getOrCreateFlixProject(project);
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
					final Optional<IProject> projectOptional = EclipseUtils.project(changePath);
					if (projectOptional.isEmpty()) {
						return;
					}
					final IProject project = projectOptional.get();
					if (project.getFile(FlixProject.FLIX_JAR_FILE_NAME).getLocation().equals(changePath)) {
						final Optional<FlixProject> flixProjectOptional = this.flix.getModel().getOrCreateFlixProject(project);
						if (flixProjectOptional.isEmpty()) {
							return;
						}
						final FlixProject flixProject = flixProjectOptional.get();
						this.flix.getLanguageToolingManager().reconnectProject(flixProject);
					} else if (project.getFile(FlixManifestToml.FLIX_MANIFEST_TOML_FILE_NAME).getLocation().equals(changePath)) {
						final Optional<FlixProject> flixProjectOptional = this.flix.getModel().getOrCreateFlixProject(project);
						if (flixProjectOptional.isEmpty()) {
							return;
						}
						final FlixProject flixProject = flixProjectOptional.get();
						flixProject.reloadManifest();
						this.flix.getLanguageToolingManager().reconnectProject(flixProject);
					}
				}
			})::dispose);
			rollback.add(this.flix.getPostResourceMonitor().onDidOpenProject().subscribe(projectOpenEvent -> {
				final IProject project = projectOpenEvent.getProject();
				final Optional<FlixProject> flixProjectOptional = this.flix.getModel().getOrCreateFlixProject(project);
				if (flixProjectOptional.isPresent()) {
					final FlixProject flixProject = flixProjectOptional.get();
					if (project.isOpen()) {
						this.flix.getLanguageToolingManager().reconnectProject(flixProject);
					} else {
						this.flix.getLanguageToolingManager().disconnectProject(flixProject);
					}
				}
			})::dispose);

			this.rollback = rollback;
		});

		FlixEditor.initPreferencesStore(getPreferenceStore());
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
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		registerImage(registry, FlixImageKey.FLIX_ICON, imageDescriptorFromPlugin(FlixConstants.PLUGIN_ID, "assets/icons/icon.png"));
		registerImage(registry, FlixImageKey.FLIX3_ICON, imageDescriptorFromPlugin(FlixConstants.PLUGIN_ID, "assets/icons/icon3.png"));
		registerImage(registry, FlixImageKey.FOLDER_ICON, ImageDescriptor.createFromURL(Utils.createUrl("platform:/plugin/org.eclipse.ui.ide/icons/full/obj16/folder.png")));
		registerImage(registry, FlixImageKey.FILE_ICON, ImageDescriptor.createFromURL(Utils.createUrl("platform:/plugin/org.eclipse.ui/icons/full/obj16/file_obj.png")));
		registerImage(registry, FlixImageKey.FLIX_LIBRARY_ICON, imageDescriptorFromPlugin(FlixConstants.PLUGIN_ID, "assets/icons/libraryicon.png"));
	}

	private void registerImage(ImageRegistry registry, FlixImageKey imageKey, ImageDescriptor descriptor) {
		registry.put(imageKey.name(), descriptor);
	}

	public static Image getImage(FlixImageKey imageKey) {
		return FlixActivator.getDefault().getImageRegistry().get(imageKey.name());
	}

	public static ImageDescriptor getImageDescriptor(FlixImageKey imageKey) {
		return FlixActivator.getDefault().getImageRegistry().getDescriptor(imageKey.name());
	}

	public static ChainedPreferenceStore getCombinedPreferenceStore() {
		return new ChainedPreferenceStore(new IPreferenceStore[] { getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
	}
}
