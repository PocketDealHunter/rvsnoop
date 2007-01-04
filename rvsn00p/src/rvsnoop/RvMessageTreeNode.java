/*
 * Class:     RvMessageTreeNode
 * Version:   $Revision$
 * Date:      $Date$
 * Copyright: Copyright © 2006-2007 Ian Phillips and Örjan Lundberg.
 * License:   Apache Software License (Version 2.0)
 */
package rvsnoop;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rvsnoop.ui.Icons;

import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvMsg;

/**
 * A {@link javax.swing.tree.TreeNode TreeNode} that wraps a Rendezvous message.
 * <p>
 * This class is designed to act as the root node in a tree model.
 *
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 * @version $Revision$, $Date$
 * @since 1.3
 */
public final class RvMessageTreeNode extends LazyTreeNode {

    private static final Log log = LogFactory.getLog(RvMessageTreeNode.class);

    private final TibrvMsg message;

    /**
     * @param message The message represented by this node.
     */
    public RvMessageTreeNode(TibrvMsg message) {
        super(null);
        this.message = message;
    }

    protected List createChildren() {
        final int numFields = message.getNumFields();
        final List children = new ArrayList(numFields);
        for (int i = 0; i < numFields; ++i)
            try {
                children.add(new RvFieldTreeNode(this, message.getFieldByIndex(i)));
            } catch (TibrvException e) {
                log.error("Problem reading message.", e);
                break;
            }
        return children;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeNode#getAllowsChildren()
     */
    public boolean getAllowsChildren() {
        return true;
    }

    public Icon getIcon() {
        return Icons.RV_MESSAGE;
    }

    public String getText() {
        return "Rendezvous Message";
    }

}
