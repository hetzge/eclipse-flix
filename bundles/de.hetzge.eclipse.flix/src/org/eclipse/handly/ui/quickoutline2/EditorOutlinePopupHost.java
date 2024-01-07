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
import org.eclipse.ui.IEditorPart;

/**
 * An editor-based outline popup host.
 */
public class EditorOutlinePopupHost
    implements IOutlinePopupHost
{
    private final IEditorPart editor;

    /**
     * Creates an outline popup host based on the given editor.
     *
     * @param editor not <code>null</code>
     */
    public EditorOutlinePopupHost(IEditorPart editor)
    {
        if (editor == null) {
			throw new IllegalArgumentException();
		}
        this.editor = editor;
    }

    /**
     * Returns the editor underlying this host.
     *
     * @return the underlying editor (never <code>null</code>)
     */
    public IEditorPart getEditor()
    {
        return this.editor;
    }

    @Override
    public Control getControl()
    {
        return this.editor.getAdapter(Control.class);
    }

    @Override
    public ISelectionProvider getSelectionProvider()
    {
        return this.editor.getSite().getSelectionProvider();
    }

    @Override
    public IEditorInput getEditorInput()
    {
        return this.editor.getEditorInput();
    }
}
