package de.hetzge.eclipse.flix;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;
import org.osgi.framework.BundleContext;

import de.hetzge.eclipse.flix.compiler.FlixCompilerProject;
import de.hetzge.eclipse.flix.editor.FlixEditor;
import de.hetzge.eclipse.flix.launch.FlixRunMainCommandHandler;
import de.hetzge.eclipse.flix.launch.FlixRunReplCommandHandler;
import de.hetzge.eclipse.flix.manifest.FlixManifestToml;
import de.hetzge.eclipse.flix.model.FlixModel;
import de.hetzge.eclipse.flix.model.FlixModelFactory;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.utils.ResourceMonitor;
import de.hetzge.eclipse.utils.EclipseUtils;
import de.hetzge.eclipse.utils.Utils;

// TODO Document service per project?! -> we have DocumentFilter
// TODO Reregister documents on LSP ready?
// TODO Catch checked exceptions with SafeRunner.run(...)
// TODO embedd flix compiler
// TODO create flix toml if missing

// TODO :eval main()
// TODO :eval selection
// TODO :eval chat

// TODO tree icons https://stackoverflow.com/questions/27718357/how-to-change-file-folder-resource-icons-from-existing-eclipse-views

// TODO fix duplicate workspace symbols

// TODO manage flix versions, flix version selection ui, model

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
		SafeRun.run(rollback -> {
			plugin = this;
			final FlixModel flixModel = FlixModelFactory.createFlixModel();
			this.flix = new Flix(flixModel);
			FlixCompilerProject.createFlixCompilerProjectIfNotExists();

			rollback.add(() -> {
				this.flix.close();
				this.flix = null;
			});

			/*
			 * Register commands ...
			 */
			rollback.add(this.flix.getCommandService().addCommand("flix.runMain", new FlixRunMainCommandHandler())::dispose);
			rollback.add(this.flix.getCommandService().addCommand("flix.cmdRepl", new FlixRunReplCommandHandler())::dispose);

			this.flix.getLanguageToolingManager().connectProjects(flixModel.getFlixProjects());
			rollback.add(this.flix.getLanguageToolingManager().startMonitor()::dispose);

			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final ResourceMonitor postResourceMonitor = this.flix.getPostResourceMonitor();
			workspace.addResourceChangeListener(postResourceMonitor, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
			rollback.add(() -> workspace.removeResourceChangeListener(postResourceMonitor));

			rollback.add(postResourceMonitor.onDidChangeFiles().subscribe(fileChangeEvent -> {
				for (final URI uri : fileChangeEvent.getFiles()) {
					final IPath changePath = URIUtil.toPath(uri);
					final Optional<IProject> projectOptional = EclipseUtils.project(changePath);
					if (projectOptional.isEmpty()) {
						return;
					}
					final IProject project = projectOptional.get();
					if (project.getFile(FlixManifestToml.FLIX_MANIFEST_TOML_FILE_NAME).getLocation().equals(changePath)) {
						final Optional<FlixProject> flixProjectOptional = flixModel.getFlixProject(project);
						if (flixProjectOptional.isEmpty()) {
							return;
						}
						final FlixProject flixProject = flixProjectOptional.get();
						flixProject.reloadManifest();
						this.flix.getLanguageToolingManager().reconnectProject(flixProject);
					}
				}
			})::dispose);
			rollback.add(postResourceMonitor.onDidOpenProject().subscribe(projectOpenEvent -> {
				final IProject project = projectOpenEvent.getProject();
				if (project.isOpen()) {
					if (FlixProject.isActiveFlixProject(project)) {

					}
				} else {
					final Optional<FlixProject> flixProjectOptional = flixModel.getFlixProject(project);
					if (flixProjectOptional.isPresent()) {


					}
				}
			})::dispose);
			rollback.add(postResourceMonitor.onDeleteProject().subscribe(project -> {
				final Optional<FlixProject> flixProjectOptional = flixModel.getFlixProject(project);
				if (flixProjectOptional.isPresent()) {
					final FlixProject flixProject = flixProjectOptional.get();
					this.flix.getLanguageToolingManager().disconnectProject(flixProject);
					flixModel.removeProject(flixProject);
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
		try {
			registerImage(registry, FlixImageKey.SRC_ICON, ImageDescriptor.createFromURL(new URL("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/packagefolder_obj.png")));
			registerImage(registry, FlixImageKey.LIB_ICON, ImageDescriptor.createFromURL(new URL("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/library_obj.png")));
			registerImage(registry, FlixImageKey.TEST_ICON, ImageDescriptor.createFromURL(new URL("platform:/plugin/org.eclipse.jdt.ui/icons/full/elcl16/th_single.png")));
		} catch (final MalformedURLException exception) {
			throw new RuntimeException(exception);
		}
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

	private void dispose() {
	}
}
