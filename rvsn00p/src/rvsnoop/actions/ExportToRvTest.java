/*
 * Class:     ExportToRvTest
 * Version:   $Revision$
 * Date:      $Date$
 * Copyright: Copyright © 2006-2007 Ian Phillips and Örjan Lundberg.
 * License:   Apache Software License (Version 2.0)
 */
package rvsnoop.actions;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.filechooser.FileFilter;

import rvsnoop.Marshaller;
import rvsnoop.Record;

/**
 * Export the current ledger selction to an RvTest format message file.
 *
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 * @version $Revision$, $Date$
 * @since 1.5
 */
final class ExportToRvTest extends ExportToFile {
    private static class RvTestMessagesFileFilter extends FileFilter {
        RvTestMessagesFileFilter() {
            super();
        }
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".msgs");
        }

        public String getDescription() {
            return "RvTest Messages Files";
        }
    }

    private static final String ID = "exportToRvTest";

    private static String NAME = "RvTest Messages File";

    private static final long serialVersionUID = -483492422948058345L;

    private static String TOOLTIP = "Export the current ledger selction to an RvTest format message file";

    private final Marshaller.Implementation marshaller = Marshaller.getImplementation(Marshaller.IMPL_RVTEST);

    public ExportToRvTest() {
        super(ID, NAME, null, new RvTestMessagesFileFilter());
        putValue(Action.SHORT_DESCRIPTION, TOOLTIP);
        putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
        setEnabled(marshaller != null);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled && marshaller != null);
    }

    /* (non-Javadoc)
     * @see rvsnoop.actions.ExportToFile#writeRecord(rvsnoop.Record, int)
     */
    protected void writeRecord(Record record, int index) {
        marshaller.marshal("", record.getMessage());
    }

}
