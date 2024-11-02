package de.hetzge.eclipse.flix.manifest;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class FlixManifestActivator implements BundleActivator {

	public static final String PLUGIN_ID = "de.hetzge.eclipse.flixmanifest";

	@Override
	public void start(BundleContext context) throws Exception {
		try (MutableFlixManifestToml tomlFile = MutableFlixManifestToml.open("file.toml")) {
			tomlFile.setValue(new String[] { "mvn-dependencies2", "test" }, "123");
			tomlFile.setValue(new String[] { "mvn-dependencies2", "test2" }, "123");
			System.out.println(tomlFile.getContent());
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
