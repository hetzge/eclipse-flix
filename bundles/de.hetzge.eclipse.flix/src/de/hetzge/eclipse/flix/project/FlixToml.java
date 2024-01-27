package de.hetzge.eclipse.flix.project;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
		final IFile file = project.getProject().getFile("flix.toml");
		final Path source = file.getLocation().makeAbsolute().toPath();
		final TomlParseResult result = Toml.parse(source);
//
//		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
//			try {
//				file.deleteMarkers(IMarker.PROBLEM, true, 0);
//			} catch (final CoreException exception) {
//				throw new RuntimeException("Failed to clear flix.toml markers", exception);
//			}
//			for (final TomlParseError error : result.errors()) {
//				try {
//					reportError(file, error.position().line(), error.getMessage());
//				} catch (final CoreException exception) {
//					throw new RuntimeException("Failed to create flix.toml marker", exception);
//				}
//			}
//		});

		return new FlixToml(result);
	}

	// TODO marker if version is missing in flix.toml

	private static void reportError(IResource resource, int line, String message) throws CoreException {
		final IMarker m = resource.createMarker(IMarker.PROBLEM);
		m.setAttribute(IMarker.LINE_NUMBER, line);
		m.setAttribute(IMarker.MESSAGE, message);
		m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
	}

}
