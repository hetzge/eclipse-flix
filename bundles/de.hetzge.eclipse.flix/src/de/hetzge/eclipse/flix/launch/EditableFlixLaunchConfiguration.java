package de.hetzge.eclipse.flix.launch;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class EditableFlixLaunchConfiguration extends FlixLaunchConfiguration {

	private final ILaunchConfigurationWorkingCopy configuration;

	public EditableFlixLaunchConfiguration(ILaunchConfigurationWorkingCopy configuration) {
		super(configuration);
		this.configuration = configuration;
	}

	@Override
	public ILaunchConfigurationWorkingCopy getConfiguration() {
		return this.configuration;
	}

	public void setEntrypoint(String entrypoint) {
		System.out.println("EditableFlixLaunchConfiguration.setEntrypoint(" + entrypoint + ")");
		this.configuration.setAttribute(ENTRYPOINT_FIELD, entrypoint != null && !entrypoint.isBlank() ? entrypoint : null);
	}

	public void setArguments(String arguments) {
		System.out.println("EditableFlixLaunchConfiguration.setArguments(" + arguments + ")");
		this.configuration.setAttribute(ARGUMENTS_FIELD, arguments != null && !arguments.isBlank() ? arguments : null);
	}
}
