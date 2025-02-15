package de.hetzge.eclipse.flix.manifest;

public final class DefaultFlixManifestToml {
	private static final String DEFAULT_FLIX_MANIFEST_TEMPLATE = "[package]\n"
			+ "name        = \"{PROJECT}\"\n"
			+ "description = \"\"\n"
			+ "version     = \"0.1.0\"\n"
			+ "flix        = \"{FLIX_VERSION}\"\n"
			+ "authors     = []";

	private DefaultFlixManifestToml() {
	}

	public static FlixManifestToml createDefaultManifestToml(String projectName, String flixVersion) {
		return FlixManifestToml.load(DEFAULT_FLIX_MANIFEST_TEMPLATE.replace("{PROJECT}", projectName).replace("{FLIX_VERSION}", flixVersion)).orElseThrow();
	}
}
