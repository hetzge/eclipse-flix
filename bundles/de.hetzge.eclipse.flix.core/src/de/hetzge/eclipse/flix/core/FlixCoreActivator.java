package de.hetzge.eclipse.flix.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class FlixCoreActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		FlixCoreActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		FlixCoreActivator.context = null;
	}

}
