//:File:    Cut.java
//:Created: Dec 28, 2005
//:Legal:   Copyright © 2005-@year@ Apache Software Foundation.
//:Legal:   Copyright © 2005-@year@ Ian Phillips.
//:License: Licensed under the Apache License, Version 2.0.
//:CVSID:   $Id$
package rvsn00p.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import rvsn00p.RecordSelection;
import rvsn00p.ui.Icons;
import rvsn00p.ui.UIUtils;
import rvsn00p.viewer.RvSnooperGUI;

import com.tibco.tibrv.TibrvException;

/**
 * Copy the currently selected record(s) to the system clipboard then remove them from the ledger.
 *
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 * @version $Revision$, $Date$
 * @since 1.4
 */
final class Cut extends AbstractAction {
    
    static String ERROR_IO = "There was an I/O error whilst writing data to the clipboard.";

    static String ERROR_RV = "There was a Rendezvous error whilst serializing the messages.";
    
    private static final String ID = "cut";
    
    static String INFO_NOTHING_SELECTED = "You must select at least one message to copy.";
    
    static String NAME = "Cut";

    private static final long serialVersionUID = 795156697514723500L;

    static String TOOLTIP = "Delete the selected records but place copies on the clipboard";
    
    public Cut() {
        super(NAME, Icons.CUT);
        putValue(Action.ACTION_COMMAND_KEY, ID);
        putValue(Action.SHORT_DESCRIPTION, TOOLTIP);
        putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
        final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, mask));
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        final RvSnooperGUI ui = RvSnooperGUI.getInstance();
        final int[] indexes = ui.getSelectedRecords();
        if (indexes == null || indexes.length == 0) {
            UIUtils.showInformation(INFO_NOTHING_SELECTED);
            return;
        }
        // First, make a local reference to the selected records.
        final List records = ui.getFilteredRecords();
        final List selected = new ArrayList(indexes.length);
        for (int i = 0, imax = indexes.length; i < imax; ++i)
            selected.add(records.get(indexes[i]));
        // Now we can take our time working on the selection.
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            final RecordSelection selection = new RecordSelection(selected);
            clipboard.setContents(selection, selection);
            ui.removeAll(selected);
        } catch (TibrvException e) {
            UIUtils.showTibrvException(ERROR_RV, e);
        } catch (IOException e) {
            UIUtils.showError(ERROR_IO, e);
        }
    }

}