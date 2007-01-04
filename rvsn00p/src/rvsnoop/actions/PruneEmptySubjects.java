/*
 * Class:     PruneEmptySubjects
 * Version:   $Revision$
 * Date:      $Date$
 * Copyright: Copyright © 2006-2007 Ian Phillips and Örjan Lundberg.
 * License:   Apache Software License (Version 2.0)
 */
package rvsnoop.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;

import rvsnoop.SubjectElement;
import rvsnoop.SubjectHierarchy;

/**
 * Remove any subject nodes from the tree that have no records in them.
 *
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 * @version $Revision$, $Date$
 * @since 1.4
 */
final class PruneEmptySubjects extends AbstractAction {

    private static final String ID = "pruneEmptySubjects";

    private static String NAME = "Prune Subjects";

    private static final long serialVersionUID = -2325639617635989562L;

    private static String TOOLTIP = "Remove any subject nodes from the tree that have no records in them";

    public PruneEmptySubjects() {
        super(NAME);
        putValue(Action.ACTION_COMMAND_KEY, ID);
        final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, mask));
        putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        putValue(Action.SHORT_DESCRIPTION, TOOLTIP);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        final SubjectHierarchy hierarchy = SubjectHierarchy.INSTANCE;
        final Enumeration e = ((DefaultMutableTreeNode) hierarchy.getRoot()).depthFirstEnumeration();
        while (e.hasMoreElements()) {
            final SubjectElement child = (SubjectElement) e.nextElement();
            if (child.isLeaf() && child.getNumRecordsHere() == 0 && child.getParent() != null)
                hierarchy.removeNodeFromParent(child);
        }
    }

}
