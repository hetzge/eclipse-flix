package de.hetzge.eclipse.flix.manifest;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import de.hetzge.eclipse.flix.core.model.FlixVersion;

public final class FlixManifestToml {

	private final TomlParseResult result;

	public FlixManifestToml(TomlParseResult result) {
		this.result = result;
	}

	public FlixVersion getFlixVersion() {
		final String versionString = this.result.getString("package.flix");
		if (versionString == null) {
			return FlixVersion.DEFAULT_VERSION;
		}
		return FlixVersion.getVersionByName(versionString).orElse(new FlixVersion(versionString, null));
	}

	public String getName() {
		return Objects.requireNonNullElse(this.result.getString("package.name"), "");
	}

	public String getDescription() {
		return Objects.requireNonNullElse(this.result.getString("package.description"), "");
	}

	public String getProjectVersion() {
		return Objects.requireNonNullElse(this.result.getString("package.version"), "");
	}

	public List<FlixDependency> getFlixDependencies() {
		final TomlTable table = this.result.getTableOrEmpty("dependencies");
		return table.keySet().stream().map(key -> {
			final String[] split = key.split(":");
			final String type = split.length == 2 ? split[0] : "";
			final String path = split.length == 2 ? split[1] : "";
			final String version = table.getString(List.of(key), () -> "");
			return new FlixDependency(type, path, version);
		}).sorted(Comparator.comparing(FlixDependency::getType).thenComparing(Comparator.comparing(FlixDependency::getPath))).toList();
	}

	public List<MavenDependency> getMavenDependencies() {
		final TomlTable table = this.result.getTableOrEmpty("mvn-dependencies");
		return table.keySet().stream().map(key -> {
			final String[] split = key.split(":");
			final String groupId = split.length == 2 ? split[0] : "";
			final String artifactId = split.length == 2 ? split[1] : "";
			final String version = table.getString(List.of(key), () -> "");
			return new MavenDependency(groupId, artifactId, version);
		}).sorted(Comparator.comparing(MavenDependency::getGroupId).thenComparing(Comparator.comparing(MavenDependency::getArtifactId))).toList();
	}

	public String calculateDependencyHash() {
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			final List<String> names = Stream.concat(
					getFlixDependencies().stream().map(it -> it.getKey() + ":" + it.getVersion()).sorted(),
					getMavenDependencies().stream().map(it -> it.getKey() + ":" + it.getVersion()).sorted())
					.collect(Collectors.toList());
			for (final String name : names) {
				messageDigest.update(name.getBytes());
			}
			final BigInteger bigInt = new BigInteger(1, messageDigest.digest());
			return bigInt.toString(16);
		} catch (final NoSuchAlgorithmException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static Optional<FlixManifestToml> load(IFile file) throws IOException {
		if (file == null) {
			return Optional.empty();
		}
		final Path source = file.getLocation().makeAbsolute().toPath();
		final TomlParseResult result = Toml.parse(source);
		return Optional.of(new FlixManifestToml(result));
	}

	public static Optional<FlixManifestToml> load(String content) {
		if (content == null) {
			return Optional.empty();
		}
		final TomlParseResult result = Toml.parse(content);
		return Optional.of(new FlixManifestToml(result));
	}

	public static class FlixDependency {
		private final String type;
		private final String path;
		private final String version;

		public FlixDependency(String type, String path, String version) {
			this.type = type;
			this.path = path;
			this.version = version;
		}

		public String getKey() {
			return getType() + ":" + getPath();
		}

		public String getType() {
			return this.type;
		}

		public String getPath() {
			return this.path;
		}

		public String getVersion() {
			return this.version;
		}

		@Override
		public String toString() {
			return "FlixDependency [type=" + this.type + ", path=" + this.path + ", version=" + this.version + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.path, this.type, this.version);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final FlixDependency other = (FlixDependency) obj;
			return Objects.equals(this.path, other.path) && Objects.equals(this.type, other.type) && Objects.equals(this.version, other.version);
		}
	}

	public static class MavenDependency {
		private final String groupId;
		private final String artifactId;
		private final String version;

		public MavenDependency(String groupId, String artifactId, String version) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
		}

		public String getKey() {
			return this.groupId + ":" + this.artifactId;
		}

		public String getGroupId() {
			return this.groupId;
		}

		public String getArtifactId() {
			return this.artifactId;
		}

		public String getVersion() {
			return this.version;
		}

		@Override
		public String toString() {
			return "MavenDependency [groupId=" + this.groupId + ", artifactId=" + this.artifactId + ", version=" + this.version + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.artifactId, this.groupId, this.version);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final MavenDependency other = (MavenDependency) obj;
			return Objects.equals(this.artifactId, other.artifactId) && Objects.equals(this.groupId, other.groupId) && Objects.equals(this.version, other.version);
		}
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
