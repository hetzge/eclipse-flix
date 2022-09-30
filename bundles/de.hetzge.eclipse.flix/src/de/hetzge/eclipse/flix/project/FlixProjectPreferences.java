package de.hetzge.eclipse.flix.project;

import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.hetzge.eclipse.flix.FlixConstants;
import de.hetzge.eclipse.flix.model.api.FlixVersion;

public final class FlixProjectPreferences extends BasePreferences {

	public FlixProjectPreferences(IProject project) {
		this(new ScopedPreferenceStore(new ProjectScope(project), FlixConstants.PLUGIN_ID));
	}

	public FlixProjectPreferences(IPreferenceStore store) {
		super(store);
	}

	public void setFlixVersion(FlixVersion version) {
		if (!version.equals(FlixVersion.CUSTOM)) {
			getStore().setValue("flixVersion", version.getKey());
		}
	}

	public Optional<FlixVersion> getFlixVersion() {
		final String key = getStore().getString("flixVersion");
		return key != null && !key.isBlank() ? Optional.of(new FlixVersion(key)) : Optional.empty();
	}
}
