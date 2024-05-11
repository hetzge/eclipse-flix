package de.hetzge.eclipse.flix.launch;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

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

	public IVMInstall getJvmInstall() {
		try {
			return JavaRuntime.computeVMInstall(this.configuration);
		} catch (final CoreException exception) {
			throw new RuntimeException(exception);
		}
	}

	public Optional<String> getEntrypoint() {
		return SafeRunner.run(() -> Optional.ofNullable(this.configuration.getAttribute(ENTRYPOINT_FIELD, (String) null)));
	}

	public Optional<String> getArguments() {
		return SafeRunner.run(() -> Optional.ofNullable(this.configuration.getAttribute(ARGUMENTS_FIELD, (String) null)));
	}
}
