// Copyright: Copyright © 2006-2010 Ian Phillips and Örjan Lundberg.
// License:   Apache Software License (Version 2.0)

package org.rvsnoop;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JFrame;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import org.bushe.swing.event.EventBus;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.ApplicationContext;
import org.rvsnoop.event.ConnectionCreatedEvent;
import org.rvsnoop.event.ConnectionDestroyedEvent;
import org.rvsnoop.event.ProjectClosingEvent;
import org.rvsnoop.event.ProjectOpenedEvent;
import org.rvsnoop.ui.SwingRunnable;
import rvsnoop.RvConnection;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.util.concurrent.Lock;
import rvsnoop.ui.UIUtils;

/**
 * A list of connections.
 * <p>
 * The list of connections is responsible for ensuring that no duplicate
 * connections are added to the list and then whenever a connection is removed
 * from the list it is stopped first.
 * <p>
 * Also, whenever a connection is added to the list it is 'activated' by calling
 * the protected method {@link RvConnection#setParentList(Connections)}.
 * <p>
 * This class wraps a sorted event list to which calls are delegated, the
 * wrapper methods handle ensuring no duplicates are added to the list and also
 * all synchronization using the Glazed Lists locking idiom.
 */
public final class Connections {

    private static final Logger logger = Logger.getLogger();

    public static void toXML(RvConnection[] connections, OutputStream stream) throws IOException {
        final XMLBuilder builder = new XMLBuilder(stream, XMLBuilder.NS_CONNECTIONS)
            .namespace(XMLBuilder.PREFIX_RENDEZVOUS, XMLBuilder.NS_RENDEZVOUS)
            .startTag("connections", XMLBuilder.NS_CONNECTIONS);
        for (int i = 0, imax = connections.length; i < imax; ++i) {
            connections[i].toXML(builder);
        }
        builder.endTag().close();
    }

    private final ApplicationContext appContext;

    private final ObservableElementList<RvConnection> list;

    @Inject
    public Connections(ApplicationContext appContext) {
        this.appContext = appContext;
        this.list = new ObservableElementList<RvConnection>(
                new SortedList<RvConnection>(
                        GlazedLists.eventList(new ArrayList<RvConnection>()),
                        new DescriptionComparator()),
                new Observer());
        AnnotationProcessor.process(this);
    }

    /**
     * As allowed by the contract for {@link java.util.Collection#add(Object)}
     * this method will not add duplicates to the list.
     *
     * @param connection The connection to add.
     * @return <code>true</code> if the argument was added, <code>false</code>
     *         otherwise.
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(RvConnection connection) {
        final Lock lock = list.getReadWriteLock().writeLock();
        lock.lock();
        try {
            if (list.contains(connection)) {
                logger.info("Ignoring attempt to add duplicate connection: %s", connection);
                return false;
            }
            logger.info("Adding connection: %s", connection);
            connection.setParentList(this);
            list.add(connection);
            EventBus.publish(new ConnectionCreatedEvent(this, connection));
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        final Lock lock = list.getReadWriteLock().writeLock();
        lock.lock();
        try {
            while (list.size() > 0) {
                final RvConnection connection = list.get(0);
                logger.info("Removing connection: %s", connection);
                connection.stop();
                connection.setParentList(null);
                list.remove(0);
            }
        } finally {
            lock.unlock();
        }
    }

    public ListModel createListModel() {
        return new EventListModel<RvConnection>(list);
    }

    /**
     * Get an existing connection.
     *
     * @param service The Rendezvous service parameter.
     * @param network The Rendezvous network parameter.
     * @param daemon The Rendezvous daemon parameter.
     * @return The existing connection, or <code>null</code> if it does not exist.
     */
    public RvConnection get(String service, String network, String daemon) {
        final Lock lock = list.getReadWriteLock().readLock();
        lock.lock();
        try {
        	for (RvConnection c : list) {
                if (c.getService().equals(service) && c.getDaemon().equals(daemon) && c.getNetwork().equals(network))
                    return c;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param connection The connection to remove.
     * @return <code>true</code> if a connection was removed,
     *         <code>false</code> otherwise.
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(RvConnection connection) {
        final Lock lock = list.getReadWriteLock().writeLock();
        lock.lock();
        try {
            if (!list.contains(connection)) { return false; }
            logger.info("Removing connection: %s", connection);
            connection.stop();
            connection.setParentList(null);
            list.remove(connection);
            EventBus.publish(new ConnectionDestroyedEvent(this, connection));
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return The number of connections.
     * @see java.util.List#size()
     */
    public int size() {
        final Lock lock = list.getReadWriteLock().readLock();
        lock.lock();
        try {
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return An array containing all of the connections.
     */
    public RvConnection[] toArray() {
        final Lock lock = list.getReadWriteLock().readLock();
        lock.lock();
        try {
            return list.toArray(new RvConnection[list.size()]);
        } finally {
            lock.unlock();
        }
    }

    @EventSubscriber
    public void onProjectClosing(ProjectClosingEvent event) {
        clear();
    }

    @EventSubscriber
    public void onProjectOpened(ProjectOpenedEvent event) {
        Future<List<RvConnection>> future = event.getSource().getConnections();
        SwingUtilities.invokeLater(new SwingRunnable<List<RvConnection>>(future, appContext) {
            @Override
            protected void onSuccess(List<RvConnection> value) {
                for (RvConnection c : value) {
                    add(c);
                }
            }
            @Override
            protected void onError(JFrame frame, Exception exception) {
                UIUtils.showError(frame, "Could not load the connections list.", exception);
            }
        });
    }

    private static class DescriptionComparator implements Comparator<RvConnection> {
        private final Collator collator = Collator.getInstance();

        public DescriptionComparator() {
            super();
        }

        public int compare(RvConnection o1, RvConnection o2) {
            return collator.compare(o1.getDescription(), o2.getDescription());
        }
    }

    private static class Observer implements ObservableElementList.Connector<RvConnection>,
            PropertyChangeListener {
        private ObservableElementList<RvConnection> list;

        Observer() {
            super();
        }

        public EventListener installListener(RvConnection element) {
            element.addPropertyChangeListener(this);
            return this;
        }

        public void propertyChange(PropertyChangeEvent event) {
            list.elementChanged((RvConnection) event.getSource());
        }

        public void setObservableElementList(ObservableElementList<RvConnection> list) {
            this.list = list;
        }

        public void uninstallListener(RvConnection element, EventListener listener) {
            element.removePropertyChangeListener(this);
        }
    }

}
