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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;

/**
 * Represents the host of an outline popup.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 *
 * @see OutlinePopup
 * @see EditorOutlinePopupHost
 */
public interface IOutlinePopupHost
{
    /**
     * Returns the SWT control for this host, or <code>null</code>
     * if the control is no longer available or has yet to be created.
     *
     * @return the SWT control or <code>null</code>
     */
    Control getControl();

    /**
     * Returns the selection provider of this host.
     *
     * @return the selection provider (never <code>null</code>)
     */
    ISelectionProvider getSelectionProvider();

    /**
     * Returns the editor input for this host, or <code>null</code> if none.
     *
     * @return the editor input or <code>null</code>
     */
    IEditorInput getEditorInput();
}
