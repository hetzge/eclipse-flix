/*******************************************************************************
 * Copyright (c) 2014, 2021 1C-Soft LLC and others.
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

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * An abstract implementation of an outline popup that supports filtering
 * based on a pattern inputed by the user.
 */
public abstract class FilteringOutlinePopup
    extends OutlinePopup
{
    private Text filterText;
    private Predicate<Object> patternMatcher;
    private Composite viewMenuButtonComposite;

    /**
     * {@inheritDoc}
     * <p>
     * <code>FilteringOutlinePopup</code> extends this method to add
     * a {@link #getPatternMatcher() pattern matcher} based filter to
     * the outline popup's tree viewer.
     * </p>
     */
    @Override
    public void init(IOutlinePopupHost host, KeyStroke invokingKeyStroke)
    {
        super.init(host, invokingKeyStroke);
        final TreeViewer treeViewer = getTreeViewer();
        treeViewer.setExpandPreCheckFilters(true);
        treeViewer.addFilter(new PatternBasedFilter());
    }

    /**
     * Returns the filter text control of this outline popup.
     *
     * @return the filter text control of this outline popup,
     *  or <code>null</code> if it has not been created yet
     */
    protected final Text getFilterText()
    {
        return this.filterText;
    }

    /**
     * Returns the current pattern matcher for this outline popup.
     *
     * @return the current pattern matcher for this outline popup,
     *  or <code>null</code> if none
     * @see #updatePatternMatcher(String)
     */
    protected final Predicate<Object> getPatternMatcher()
    {
        return this.patternMatcher;
    }

    @Override
    protected Control getFocusControl()
    {
        return this.filterText;
    }

    @Override
    protected void setTabOrder(Composite composite)
    {
        this.viewMenuButtonComposite.setTabList(new Control[] { this.filterText });
        composite.setTabList(new Control[] { this.viewMenuButtonComposite,
            getTreeViewer().getTree() });
    }

    /**
     * Creates a tree viewer for this outline popup. The viewer has no input,
     * no content provider, a default label provider, no sorter, and no filters.
     * This method is called once, when the popup's control is created.
     * <p>
     * This implementation returns a new instance of
     * {@link FilteringOutlineTreeViewer}.
     * </p>
     */
    @Override
    protected TreeViewer createTreeViewer(Composite parent)
    {
        final TreeViewer baseTreeViewer = super.createTreeViewer(parent);
        return new FilteringOutlineTreeViewer(baseTreeViewer.getTree());
    }

    @Override
    protected Control createTitleMenuArea(Composite parent)
    {
        this.viewMenuButtonComposite = (Composite)super.createTitleMenuArea(parent);
        return this.viewMenuButtonComposite;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation creates the {@link #getFilterText() filter text}
     * control. It uses {@link #createFilterText(Composite)} to create the control.
     * </p>
     */
    @Override
    protected Control createTitleControl(Composite parent)
    {
        this.filterText = createFilterText(parent);
        return this.filterText;
    }

    /**
     * Creates the text control to be used for entering the filter pattern.
     * <p>
     * This implementation creates a text control that:
     * </p>
     * <ul>
     * <li>Invokes {@link #updatePatternMatcher(String)} when the text is modified.</li>
     * <li>Invokes {@link #gotoSelectedElement()} when the ENTER key is pressed.</li>
     * <li>Sets the keyboard focus to the tree viewer when the DOWN ARROW or
     * UP ARROW key is pressed.</li>
     * <li>Invokes {@link #close()} when the ESC key is pressed.</li>
     * </ul>
     * <p>
     * If an {@link #getInvokingKeyStroke() invoking key} is set,
     * this implementation adds the {@link #getInvokingKeyListener()
     * invoking key listener} to the created control.
     * </p>
     *
     * @param parent the parent composite (never <code>null</code>)
     * @return the created filter text control (not <code>null</code>)
     */
    protected Text createFilterText(Composite parent)
    {
        final Text filterText = new Text(parent, SWT.NONE);
        Dialog.applyDialogFont(filterText);

        final GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.CENTER;
        filterText.setLayoutData(data);

        filterText.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == 0x0D) // ENTER
                {
                    gotoSelectedElement();
                }
                else if (e.keyCode == SWT.ARROW_DOWN
                    || e.keyCode == SWT.ARROW_UP)
                {
                    getTreeViewer().getControl().setFocus();
                }
                else if (e.character == 0x1B) // ESC
                {
                    close();
                }
            }
        });

        if (getInvokingKeyStroke() != null) {
			filterText.addKeyListener(getInvokingKeyListener());
		}

        filterText.addModifyListener(e -> updatePatternMatcher(
            ((Text)e.widget).getText()));

        return filterText;
    }

    /**
     * Updates the current pattern matcher to an instance {@link
     * #createPatternMatcher(String) created} for the given pattern
     * and {@link #patternMatcherUpdated() notifies} of the update.
     *
     * @param pattern the pattern string (not <code>null</code>)
     * @see #getPatternMatcher()
     */
    protected final void updatePatternMatcher(String pattern)
    {
        this.patternMatcher = createPatternMatcher(pattern);
        patternMatcherUpdated();
    }

    /**
     * Returns a new pattern matcher based on the given pattern.
     * May return <code>null</code> if no filtering is required.
     * <p>
     * This implementation returns <code>null</code> if the pattern is an
     * empty string. Otherwise, it appends '*' to the pattern if the pattern
     * does not already end with '*', and returns an {@link ElementMatcher}
     * based on a {@link StringMatcher} for the pattern. Case-insensitive
     * matching is enabled if, and only if, the pattern is all lower-case.
     * </p>
     *
     * @param pattern the pattern string (not <code>null</code>)
     * @return the created pattern matcher, or <code>null</code>
     *  if no filtering is required
     */
    protected Predicate<Object> createPatternMatcher(String pattern)
    {
        final int length = pattern.length();
        if (length == 0) {
			return null;
		}
        if (pattern.charAt(length - 1) != '*') {
			pattern = pattern + '*';
		}
        return new ElementMatcher(new StringMatcher(pattern,
            pattern.toLowerCase().equals(pattern)));
    }

    /**
     * Notifies that the pattern matcher has been updated.
     * <p>
     * This implementation refreshes the tree viewer, expands all nodes
     * of the tree, and {@link #selectFirstMatch() selects} the first
     * matching element.
     * </p>
     */
    protected void patternMatcherUpdated()
    {
        final TreeViewer treeViewer = getTreeViewer();
        try
        {
            treeViewer.getControl().setRedraw(false);
            treeViewer.refresh();
            treeViewer.expandAll();
        }
        finally
        {
            treeViewer.getControl().setRedraw(true);
        }
        selectFirstMatch();
    }

    /**
     * Selects the first element that matches the current filter pattern.
     * <p>
     * This implementation starts the search from the {@link #getFocalElement()
     * focal element}. If there is no focal element, the search is started from
     * the root of the tree.
     * </p>
     */
    protected void selectFirstMatch()
    {
        final Object focalElement = getFocalElement();
        Object focalItem = null;
        final TreeViewer treeViewer = getTreeViewer();
        if (focalElement != null) {
			focalItem = treeViewer.testFindItem(focalElement);
		}
        TreeItem item;
        final Tree tree = treeViewer.getTree();
        if (focalItem instanceof TreeItem) {
			item = findItem(new TreeItem[] { (TreeItem)focalItem });
		} else {
			item = findItem(tree.getItems());
		}
        if (item == null) {
			treeViewer.setSelection(StructuredSelection.EMPTY);
		} else
        {
            tree.setSelection(item);
            tree.showItem(item);
        }
    }

    /**
     * Returns the current focal element for this outline popup.
     * <p>
     * This implementation returns the {@link #getInitialSelection()
     * initially selected} element. Subclasses may override.
     * </p>
     *
     * @return the current focal element for this outline popup,
     *  or <code>null</code> if none
     */
    protected Object getFocalElement()
    {
        return getInitialSelection();
    }

    private TreeItem findItem(TreeItem[] items)
    {
        return findItem(items, null, true);
    }

    private TreeItem findItem(TreeItem[] items, TreeItem[] toBeSkipped,
        boolean allowToGoUp)
    {
        if (this.patternMatcher == null) {
			return items.length > 0 ? items[0] : null;
		}

        // First search at same level
        for (int i = 0; i < items.length; i++)
        {
            final TreeItem item = items[i];
            final Object element = item.getData();
            if (this.patternMatcher.test(element)) {
				return item;
			}
        }

        // Go one level down for each item
        for (int i = 0; i < items.length; i++)
        {
            final TreeItem item = items[i];
            final TreeItem foundItem = findItem(selectItems(item.getItems(),
                toBeSkipped), null, false);
            if (foundItem != null) {
				return foundItem;
			}
        }

        if (!allowToGoUp || items.length == 0) {
			return null;
		}

        // Go one level up (parent is the same for all items)
        final TreeItem parentItem = items[0].getParentItem();
        if (parentItem != null) {
			return findItem(new TreeItem[] { parentItem }, items, true);
		}

        // Check root elements
        return findItem(selectItems(items[0].getParent().getItems(), items),
            null, false);
    }

    private static boolean canSkip(TreeItem item, TreeItem[] toBeSkipped)
    {
        if (toBeSkipped == null) {
			return false;
		}

        for (int i = 0; i < toBeSkipped.length; i++)
        {
            if (toBeSkipped[i] == item) {
				return true;
			}
        }
        return false;
    }

    private static TreeItem[] selectItems(TreeItem[] items,
        TreeItem[] toBeSkipped)
    {
        if (toBeSkipped == null || toBeSkipped.length == 0) {
			return items;
		}

        int j = 0;
        for (int i = 0; i < items.length; i++)
        {
            final TreeItem item = items[i];
            if (!canSkip(item, toBeSkipped)) {
				items[j++] = item;
			}
        }
        if (j == items.length) {
			return items;
		}

        final TreeItem[] result = new TreeItem[j];
        System.arraycopy(items, 0, result, 0, j);
        return result;
    }

    /**
     * Extends {@link OutlinePopup.OutlineTreeViewer} to allow expanding
     * any tree item when the pattern-based filter is active.
     */
    protected class FilteringOutlineTreeViewer
        extends OutlineTreeViewer
    {
        /**
         * Creates a new tree viewer on the given tree control.
         * Sets auto-expand level to <code>ALL_LEVELS</code>.
         *
         * @param tree the tree control (not <code>null</code>)
         */
        public FilteringOutlineTreeViewer(Tree tree)
        {
            super(tree);
        }

        /**
         * {@inheritDoc}
         * <p>
         * <code>FilteringOutlineTreeViewer</code> extends this method to allow
         * expanding any tree item when the pattern-based filter is active.
         * </p>
         */
        @Override
        protected boolean canExpand(TreeItem item)
        {
            if (FilteringOutlinePopup.this.patternMatcher != null) {
				return true;
			}
            return super.canExpand(item);
        }
    }

    /**
     * A pattern-based element matcher for the outline popup. Passes the {@link
     * ElementMatcher#getText(Object) text} (by default, the label string)
     * obtained for the given outline element to the underlying string matcher.
     */
    protected class ElementMatcher
        implements Predicate<Object>
    {
        private final Predicate<String> stringMatcher;

        /**
         * Creates a new element matcher based on the given string matcher.
         *
         * @param stringMatcher not <code>null</code>
         */
        public ElementMatcher(Predicate<String> stringMatcher)
        {
            if (stringMatcher == null) {
				throw new IllegalArgumentException();
			}
            this.stringMatcher = stringMatcher;
        }

        @Override
        public final boolean test(Object element)
        {
            if (element == null) {
				return false;
			}
            return this.stringMatcher.test(getText(element));
        }

        /**
         * Returns the text for the given outline element.
         * <p>
         * Default implementation returns the label string obtained from
         * the tree viewer's label provider.
         * </p>
         *
         * @param element the outline element (never <code>null</code>)
         * @return the text for the given outline element,
         *  or <code>null</code> if no text can be obtained
         */
        protected String getText(Object element)
        {
            final IBaseLabelProvider labelProvider =
                getTreeViewer().getLabelProvider();
            if (labelProvider instanceof ILabelProvider) {
				return ((ILabelProvider)labelProvider).getText(element);
			} else if (labelProvider instanceof IStyledLabelProvider) {
				return ((IStyledLabelProvider)labelProvider).getStyledText(
                    element).toString();
			} else if (labelProvider instanceof DelegatingStyledCellLabelProvider) {
				return ((DelegatingStyledCellLabelProvider)labelProvider).getStyledStringProvider().getStyledText(
                    element).toString();
			}
            return null;
        }
    }

    /**
     * A string pattern matcher that supports '*' and '?' wildcards.
     */
    protected static class StringMatcher
        implements Predicate<String>
    {
        private final String expression;
        private final boolean ignoreCase;
        private Pattern pattern;

        /**
         * Creates a new string matcher based on the given pattern.
         * The pattern may contain '*' for zero or more characters and
         * '?' for exactly one character.
         *
         * @param pattern the pattern string (not <code>null</code>)
         * @param ignoreCase whether case-insensitive matching is enabled
         */
        public StringMatcher(String pattern, boolean ignoreCase)
        {
            this.expression = translatePattern(pattern);
            this.ignoreCase = ignoreCase;
        }

        @Override
        public final boolean test(String text)
        {
            if (text == null) {
				return false;
			}
            return getPattern().matcher(text).find();
        }

        /**
         * Translates the given pattern into a regular expression.
         * <p>
         * This implementation always returns a regular expression
         * that starts with '^'.
         * </p>
         *
         * @param pattern the pattern string (not <code>null</code>)
         * @return the regular expression corresponding to the pattern
         *  (never <code>null</code>)
         */
        protected String translatePattern(String pattern)
        {
            String expression = pattern.replaceAll("\\(", "\\\\("); //$NON-NLS-1$ //$NON-NLS-2$
            expression = expression.replaceAll("\\)", "\\\\)"); //$NON-NLS-1$ //$NON-NLS-2$
            expression = expression.replaceAll("\\[", "\\\\["); //$NON-NLS-1$ //$NON-NLS-2$
            expression = expression.replaceAll("\\]", "\\\\]"); //$NON-NLS-1$ //$NON-NLS-2$
            expression = expression.replaceAll("\\{", "\\\\{"); //$NON-NLS-1$ //$NON-NLS-2$
            expression = expression.replaceAll("\\}", "\\\\}"); //$NON-NLS-1$ //$NON-NLS-2$
            expression = expression.replaceAll("\\*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
            expression = expression.replaceAll("\\?", "."); //$NON-NLS-1$ //$NON-NLS-2$
            if (!expression.startsWith("^"))
			 { //$NON-NLS-1$
				expression = "^" + expression; //$NON-NLS-1$
			}
            return expression;
        }

        private Pattern getPattern()
        {
            if (this.pattern == null)
            {
                if (this.ignoreCase) {
					this.pattern = Pattern.compile(this.expression,
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
				} else {
					this.pattern = Pattern.compile(this.expression);
				}
            }
            return this.pattern;
        }
    }

    private class PatternBasedFilter
        extends ViewerFilter
    {
        @Override
        public boolean select(Viewer viewer, Object parentElement,
            Object element)
        {
            if (FilteringOutlinePopup.this.patternMatcher == null) {
				return true;
			}

            if (FilteringOutlinePopup.this.patternMatcher.test(element)) {
				return true;
			}

            return hasUnfilteredChild((TreeViewer)viewer, element);
        }

        private boolean hasUnfilteredChild(TreeViewer treeViewer,
            Object element)
        {
            // This works only because 'expandPreCheckFilters' was set to true
            return treeViewer.isExpandable(element);
        }
    }
}
