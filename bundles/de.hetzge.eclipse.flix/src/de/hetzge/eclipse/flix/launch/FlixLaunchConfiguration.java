package de.hetzge.eclipse.flix.launch;

import java.util.Optional;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.ILaunchConfiguration;

public class FlixLaunchConfiguration {

	static final String ENTRYPOINT_FIELD = "entrypoint";
	static final String ARGUMENTS_FIELD = "arguments";

	private final ILaunchConfiguration configuration;

	public FlixLaunchConfiguration(ILaunchConfiguration configuration) {
		this.configuration = configuration;
	}

	public ILaunchConfiguration getConfiguration() {
		return this.configuration;
	}

	public Optional<String> getEntrypoint() {
		return SafeRunner.run(() -> Optional.ofNullable(this.configuration.getAttribute(ENTRYPOINT_FIELD, (String) null)));
	}

	public Optional<String> getArguments() {
		return SafeRunner.run(() -> Optional.ofNullable(this.configuration.getAttribute(ARGUMENTS_FIELD, (String) null)));
	}
}
