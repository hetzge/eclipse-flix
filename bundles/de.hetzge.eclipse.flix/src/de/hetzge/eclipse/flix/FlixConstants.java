package de.hetzge.eclipse.flix;

import de.hetzge.eclipse.flix.model.api.FlixVersion;

public final class FlixConstants {

	public static final String PLUGIN_ID = "de.hetzge.eclipse.flix"; //$NON-NLS-1$
	public static final String LANGUAGE_ID = "flix"; //$NON-NLS-1$

	public static final FlixVersion FLIX_DEFAULT_VERSION = FlixVersion.VERSION_0_30_0;

	public static final String FLIX_BUILDER_ID = "de.hetzge.eclipse.flix.flixBuilder"; //$NON-NLS-1$

	public static final String LAUNCH_CONFIGURATION_TYPE_ID = "de.hetzge.eclipse.flix.launchConfigurationType"; //$NON-NLS-1$
	public static final String TEST_LAUNCH_CONFIGURATION_TYPE_ID = "de.hetzge.eclipse.flix.testLaunchConfigurationType"; //$NON-NLS-1$

	public static final String FLIX_ICON_IMAGE_KEY = "de.hetzge.eclipse.flix.image.flix.icon.png"; //$NON-NLS-1$
	public static final String FOLDER_ICON_IMAGE_KEY = "de.hetzge.eclipse.flix.image.folder.icon.png"; //$NON-NLS-1$

	private FlixConstants() {
		// private constants class constructor
	}

}
