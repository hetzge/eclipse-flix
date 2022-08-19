package de.hetzge.eclipse.flix.model;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.context.Context;
import org.eclipse.handly.model.IElementChangeEvent;
import org.eclipse.handly.model.IElementDelta;
import org.eclipse.handly.model.impl.support.ElementChangeEvent;
import org.eclipse.handly.model.impl.support.ElementManager;
import org.eclipse.handly.model.impl.support.IModelManager;
import org.eclipse.handly.model.impl.support.INotificationManager;
import org.eclipse.handly.model.impl.support.NotificationManager;

import de.hetzge.eclipse.flix.FlixLogger;

public class FlixModelManager implements IModelManager, IResourceChangeListener {

	private final FlixModel model;
	private final ElementManager elementManager;
	private final NotificationManager notificationManager;

	public FlixModelManager(FlixModel model, ElementManager elementManager, NotificationManager notificationManager) {
		this.model = model;
		this.elementManager = elementManager;
		this.notificationManager = notificationManager;
	}

	@Override
	public FlixModel getModel() {
		System.out.println("FlixModelManager.getModel()");
		return this.model;
	}

	@Override
	public ElementManager getElementManager() {
		return this.elementManager;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			try {
				final FlixDeltaProcessor deltaProcessor = new FlixDeltaProcessor();
				event.getDelta().accept(deltaProcessor);

				final IElementDelta[] deltas = deltaProcessor.getDeltas();
				if (deltas.length > 0) {
					this.notificationManager.fireElementChangeEvent(new ElementChangeEvent(IElementChangeEvent.POST_CHANGE, deltas));
				}
			} catch (final CoreException exception) {
				FlixLogger.logError(exception);
			}
		}
	}

	public NotificationManager getNotificationManager() {
		return this.notificationManager;
	}

	public static FlixModelManager create() {
		System.out.println("FlixModelManager.create()");
		final NotificationManager notificationManager = new NotificationManager();

		/*
		 * "A context, represented in Handly by the IContext interface, supplies values
		 * (data objects or services) associated with keys. Two kinds of keys are
		 * supported: org.eclipse.handly.util.Property and java.lang.Class, both of
		 * which allow the client to specify the type of requested value, which makes
		 * contexts completely type safe. The model context provides information and
		 * services pertaining to the model." Source:
		 * https://github.com/pisv/gethandly/wiki/Step-One
		 */
		final Context context = new Context();
		context.bind(INotificationManager.class).to(notificationManager);

		final FlixModel model = new FlixModel(context);
		final ElementManager elementManager = new ElementManager(new FlixModelCache());
		return new FlixModelManager(model, elementManager, notificationManager);
	}
}
