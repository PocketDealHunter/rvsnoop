/*
 * Class:     MatcherListCellRenderer
 * Version:   $Revision$
 * Date:      $Date$
 * Copyright: Copyright © 2007-2007 Ian Phillips.
 * License:   Apache Software License (Version 2.0)
 */
package org.rvsnoop.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import rvsnoop.RvConnection;
import rvsnoop.State;

/**
 * A renderer for connections.
 *
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 * @version $Revision$, $Date$
 * @since 1.7
 */
public final class ConnectionListCellRenderer extends JPanel implements ListCellRenderer {

    private static final long serialVersionUID = 4673460542680039876L;

    private  static final int ICON_INDENT = 32;

    private static final Icon PAUSED = new ImageIcon("/resources/icons/connectionPaused.png");
    private static final Icon STARTED = new ImageIcon("/resources/icons/connectionStarted.png");
    private static final Icon STOPPED = new ImageIcon("/resources/icons/connectionStopped.png");

    private final JLabel daemonLabel = new DefaultListCellRenderer();

    private boolean hidingDefaultValues;

    private final JLabel nameLabel = new DefaultListCellRenderer();

    private final JLabel networkLabel = new DefaultListCellRenderer();

    private final JLabel serviceLabel = new DefaultListCellRenderer();

    public ConnectionListCellRenderer(boolean hidingDefaultValues) {
        this.hidingDefaultValues = hidingDefaultValues;
        setBorder(new EmptyBorder(4, 4, 4, 4));
        setOpaque(true);
        Font font = nameLabel.getFont();
        font = font.deriveFont(font.getSize2D() - 2.0f);
        final Color c = nameLabel.getForeground().brighter().brighter();
        serviceLabel.setFont(font);
        networkLabel.setFont(font);
        daemonLabel.setFont(font);
        serviceLabel.setForeground(c);
        networkLabel.setForeground(c);
        daemonLabel.setForeground(c);
        nameLabel.setOpaque(false);
        serviceLabel.setOpaque(false);
        networkLabel.setOpaque(false);
        daemonLabel.setOpaque(false);
        configureLayout();
    }

    private void configureLayout() {
        // Layout
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setHonorsVisibility(true);
        // Horizontal group
        GroupLayout.ParallelGroup hgp = layout.createParallelGroup();
        layout.setHorizontalGroup(hgp);
        hgp.addComponent(nameLabel)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(ICON_INDENT, ICON_INDENT)
                .addGroup(layout.createParallelGroup()
                    .addComponent(serviceLabel)
                    .addComponent(networkLabel)
                    .addComponent(daemonLabel)));
        // Vertical group
        GroupLayout.SequentialGroup vgp = layout.createSequentialGroup();
        layout.setVerticalGroup(vgp);
        vgp.addComponent(nameLabel)
            .addComponent(serviceLabel)
            .addComponent(networkLabel)
            .addComponent(daemonLabel);
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof RvConnection) {
            final RvConnection c = (RvConnection) value;
            final State s = c.getState();
            if (s == State.STARTED) {
                nameLabel.setIcon(STARTED);
            } else if (s == State.STOPPED) {
                nameLabel.setIcon(STOPPED);
            } else if (s == State.PAUSED) {
                nameLabel.setIcon(PAUSED);
            }
            nameLabel.setText(c.getDescription());
            setDetails(serviceLabel, "Service: ", c.getService(),
                    hidingDefaultValues ? RvConnection.DEFAULT_SERVICE : null);
            setDetails(networkLabel, "Network: ", c.getNetwork(),
                    hidingDefaultValues ? RvConnection.DEFAULT_NETWORK : null);
            setDetails(daemonLabel, "Daemon: ", c.getDaemon(),
                    hidingDefaultValues ? RvConnection.DEFAULT_DAEMON : null);
            setToolTipText(getToolTipText(c));
        } else {
            nameLabel.setIcon(null);
            serviceLabel.setText(" ");
            networkLabel.setText(" ");
            daemonLabel.setText(" ");
        }
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }

    private String getToolTipText(final RvConnection connection) {
        final StringBuilder builder = new StringBuilder();
        for (String s : connection.getSubjects()) {
            builder.append(s).append('\n');
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        } else {
            builder.append("No subjects subscribed to");
        }
        return builder.toString();
    }

    private void setDetails(JLabel label, String prefix, String value, String def) {
        if (value.equals(def)) {
            label.setVisible(false);
        } else {
            label.setText(prefix + value);
            label.setVisible(true);
        }
    }

    /**
     * @param hidingDefaultValues the hidingDefaultValues to set
     */
    public void setHidingDefaultValues(boolean hidingDefaultValues) {
        this.hidingDefaultValues = hidingDefaultValues;
        revalidate();
    }

}
