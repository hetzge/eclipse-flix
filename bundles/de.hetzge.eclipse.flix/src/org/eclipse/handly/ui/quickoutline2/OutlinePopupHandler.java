/*******************************************************************************
 * Copyright (c) 2014, 2018 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.eclipse.handly.ui.quickoutline2;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.keys.IBindingService;

/**
 *  An abstract implementation of a handler that opens an outline popup.
 */
public abstract class OutlinePopupHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final IOutlinePopupHost host = getOutlinePopupHost(event);
        if (host == null) {
			return null;
		}
        final OutlinePopup outlinePopup = createOutlinePopup();
        outlinePopup.init(host, getInvokingKeyStroke(event));
        outlinePopup.open();
        return null;
    }

    /**
     * Creates a new instance of the outline popup.
     *
     * @return the created oultine popup (not <code>null</code>)
     */
    protected abstract OutlinePopup createOutlinePopup();

    /**
     * Returns the outline popup host for the given execution event.
     * <p>
     * This implementation returns a host based on the active editor,
     * or <code>null</code> if no editor is currently active.
     * <p>
     *
     * @param event the execution event (never <code>null</code>)
     * @return the outline popup host, or <code>null</code> if none
     */
    protected IOutlinePopupHost getOutlinePopupHost(ExecutionEvent event)
    {
        final IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (editor == null) {
			return null;
		}
        return new EditorOutlinePopupHost(editor);
    }

    /**
     * Returns the invoking keystroke for the given execution event.
     * <p>
     * This implementation returns the first keystroke bound to
     * the event's {@link ExecutionEvent#getCommand() command}.
     * </p>
     *
     * @param event the execution event (never <code>null</code>)
     * @return the invoking keystroke, or <code>null</code> if none
     */
    protected KeyStroke getInvokingKeyStroke(ExecutionEvent event)
    {
        final IBindingService bindingService =
            PlatformUI.getWorkbench().getService(
		    IBindingService.class);
        final TriggerSequence[] bindings = bindingService.getActiveBindingsFor(
            new ParameterizedCommand(event.getCommand(), null));
        for (final TriggerSequence binding : bindings)
        {
            if (binding instanceof KeySequence)
            {
                final KeyStroke[] keyStrokes = ((KeySequence)binding).getKeyStrokes();
                if (keyStrokes.length > 0) {
					return keyStrokes[0];
				}
            }
        }
        return null;
    }
}
