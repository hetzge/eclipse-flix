package de.hetzge.eclipse.flix.project;

import java.io.IOException;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

abstract class BasePreferences {

	private final IPreferenceStore store;

	public BasePreferences(IPreferenceStore store) {
		this.store = store;
	}

	public IPreferenceStore getStore() {
		return this.store;
	}

	public void save() {
		try {
			if (this.store instanceof IPersistentPreferenceStore) {
				final IPersistentPreferenceStore persistentPreferenceStore = (IPersistentPreferenceStore) this.store;
				persistentPreferenceStore.save();
			}
		} catch (final IOException exception) {
			throw new IllegalStateException("Failed to save preferences", exception);
		}
	}
}
