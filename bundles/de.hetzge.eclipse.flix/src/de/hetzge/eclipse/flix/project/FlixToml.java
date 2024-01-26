package de.hetzge.eclipse.flix.project;

import java.io.IOException;
import java.nio.file.Path;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.FlixProject;
import de.hetzge.eclipse.flix.model.FlixVersion;

public final class FlixToml {

	private final TomlParseResult result;

	public FlixToml(TomlParseResult result) {
		this.result = result;
	}

	public FlixVersion getFlixVersion() {
		final String versionString = this.result.getString("package.flix");
		if (versionString == null) {
			return FlixConstants.FLIX_DEFAULT_VERSION;
		}
		return new FlixVersion(versionString);
	}

	public static FlixToml load(FlixProject project) throws IOException {
		final Path source = project.getProject().getFile("flix.toml").getLocation().makeAbsolute().toPath();
		final TomlParseResult result = Toml.parse(source);
		return new FlixToml(result);
	}

}
