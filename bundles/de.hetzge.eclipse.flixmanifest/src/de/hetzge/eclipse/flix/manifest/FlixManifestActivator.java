package de.hetzge.eclipse.flix.manifest;

import java.util.Objects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class FlixManifestActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "de.hetzge.eclipse.flixmanifest";

	private static FlixManifestActivator plugin;

	public static FlixManifestActivator getDefault() {
		Objects.requireNonNull(plugin, "Flix manifest plugin is not initialized");
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		this.plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.plugin = null;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		registerImage(registry, FlixManifestImageKey.MAVEN_REPOSITORY_COM, imageDescriptorFromPlugin(PLUGIN_ID, "assets/icons/mavenrepositorycom.png"));
		registerImage(registry, FlixManifestImageKey.GITHUB, imageDescriptorFromPlugin(PLUGIN_ID, "assets/icons/github.png"));
	}

	private void registerImage(ImageRegistry registry, FlixManifestImageKey imageKey, ImageDescriptor descriptor) {
		registry.put(imageKey.name(), descriptor);
	}

	public static Image getImage(FlixManifestImageKey imageKey) {
		return FlixManifestActivator.getDefault().getImageRegistry().get(imageKey.name());
	}

}
