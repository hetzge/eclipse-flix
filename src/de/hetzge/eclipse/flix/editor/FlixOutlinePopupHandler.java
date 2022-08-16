/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
package de.hetzge.eclipse.flix.editor;

import org.eclipse.handly.ui.quickoutline.OutlinePopup;
import org.eclipse.handly.ui.quickoutline.OutlinePopupHandler;

/**
 * A handler that opens the TypeScript outline popup.
 */
public class FlixOutlinePopupHandler
    extends OutlinePopupHandler
{
    @Override
    protected OutlinePopup createOutlinePopup()
    {
        return new FlixOutlinePopup();
    }
}
