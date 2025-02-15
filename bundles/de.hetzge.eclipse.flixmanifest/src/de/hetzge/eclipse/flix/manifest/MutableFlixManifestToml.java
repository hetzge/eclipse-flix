package de.hetzge.eclipse.flix.manifest;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.extism.sdk.HostFunction;
import org.extism.sdk.HostUserData;
import org.extism.sdk.LibExtism;
import org.extism.sdk.LibExtism.ExtismValType;
import org.extism.sdk.Plugin;
import org.extism.sdk.manifest.Manifest;
import org.extism.sdk.wasm.ByteArrayWasmSource;

public class MutableFlixManifestToml implements AutoCloseable {
	private static final Manifest MANIFEST;
	static {
		try {
			final IPath path = Path.fromOSString("rusttoml/target/wasm32-unknown-unknown/release/rusttoml.wasm");
			final URL url = FileLocator.find(Platform.getBundle(FlixManifestActivator.PLUGIN_ID), path);
			final ByteArrayWasmSource wasmSource = new ByteArrayWasmSource(null, url.openStream().readAllBytes(), null);
			MANIFEST = new Manifest(List.of(wasmSource));
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	private final Plugin plugin;

	private MutableFlixManifestToml(Plugin plugin) {
		this.plugin = plugin;
	}

	public void setContent(String toml) {
		this.plugin.call("set_toml", toml);
	}

	public String getContent() {
		return this.plugin.call("get_toml", "");
	}

	public void setValue(String[] path, String value) {
		this.plugin.call("set_value", "{\"path\":[" + Arrays.stream(path).map(it -> "\"" + it + "\"").collect(Collectors.joining(",")) + "],\"value\":\"" + value + "\"}");
	}

	public void unsetValue(String[] path) {
		this.plugin.call("unset_value", "{\"path\":[" + Arrays.stream(path).map(it -> "\"" + it + "\"").collect(Collectors.joining(",")) + "]}");
	}

	@Override
	public void close() {
		this.plugin.close();
	}

	public static final MutableFlixManifestToml open(String toml) {
		final HostFunction<HostUserData> hostLogFunction = new HostFunction<>("host_log", new ExtismValType[] { LibExtism.ExtismValType.I64 }, new ExtismValType[] { LibExtism.ExtismValType.I64 }, (p, params, returns, data) -> {
			System.out.println("LOG: " + p.inputString(params[0]));
		}, Optional.empty());
		final Plugin plugin = new Plugin(MANIFEST, true, new HostFunction[] { hostLogFunction });
		final MutableFlixManifestToml tomlFile = new MutableFlixManifestToml(plugin);
		try {
			tomlFile.setContent(toml);
		} catch (final Exception exception) {
			plugin.close();
			throw new RuntimeException(exception);
		}
		return tomlFile;
	}

}