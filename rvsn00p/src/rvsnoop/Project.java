//:File:    Project.java
//:Created: Jan 4, 2006
//:Legal:   Copyright © 2006-@year@ Apache Software Foundation.
//:Legal:   Copyright © 2006-@year@ Ian Phillips.
//:License: Licensed under the Apache License, Version 2.0.
//:FileID:  $Id$
package rvsnoop;

import java.awt.Color;
import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import rvsnoop.RecordMatcher;
import rvsnoop.ui.UIManager;

/**
 * Handles all of the project specific settings.
 *
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 * @version $Revision$, $Date$
 * @since 1.5
 */
public final class Project extends XMLConfigFile {

    private static final String COLOUR = "colour";
    private static Project currentProject;
    private static final String ROOT = "rvsnoopProject";
    private static final String RV_CONNECTION = "connection";
    private static final String RV_DAEMON = "daemon";
    private static final String RV_DESCRIPTION = "description";
    private static final String RV_NETWORK = "network";
    private static final String RV_SERVICE = "service";
    private static final String SUBJECT = "subject";
    private static final String SUBJECT_EXPANDED = "expanded";
    private static final String SUBJECT_NAME = "name";
    private static final String SUBJECT_SELECTED = "selected";
    private static final String SUBJECTS = "subjects";
    private static final String TYPE = "recordType";
    private static final String TYPE_NAME = "name";
    private static final String TYPE_MATCHER = "matcher";
    private static final String TYPE_MATCHER_NAME = "name";
    private static final String TYPE_MATCHER_VALUE = "value";
    private static final String TYPE_SELECTED = "selected";

    /**
     * @return Returns the currentProject.
     */
    public static Project getCurrentProject() {
        return currentProject;
    }

    /**
     * @param currentProject The currentProject to set.
     */
    public static void setCurrentProject(Project currentProject) {
        Connections.getInstance().clear();
        SubjectHierarchy.INSTANCE.removeAll();
        Project.currentProject = currentProject;
        if (currentProject != null) {
            currentProject.load();
            RecentProjects.INSTANCE.add(currentProject);
        }
    }

    public Project(File file) {
        super(file);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (file != null)
            return obj instanceof Project && file.equals(((Project) obj).file);
        else
            return obj instanceof Project && ((Project) obj).file == null;
    }

    protected Document getDocument() {
        final Element root = new Element(ROOT);
        storeTypes(root);
        storeSubjectTree(root);
        storeConnections(root);
        return new Document(root);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return file.hashCode();
    }

    protected void load(Element root) {
        loadSubjectTree(root);
        loadTypes(root);
        final RvConnection[] conns = loadConnections(root);
        final Connections connlist = Connections.getInstance();
        for (int i = 0, imax = conns.length; i < imax; ++i) {
            connlist.add(conns[i]);
            conns[i].start();
        }
    }

    public static RvConnection[] loadConnections(Element parent) {
        final Elements elements = parent.getChildElements(RV_CONNECTION);
        final RvConnection[] conns = new RvConnection[elements.size()];
        for (int i = 0, imax = elements.size(); i < imax; ++i) {
            final Element element = elements.get(i);
            final String description = getString(element, RV_DESCRIPTION);
            final String service = getString(element, RV_SERVICE);
            final String network = getString(element, RV_NETWORK);
            final String daemon = getString(element, RV_DAEMON);
            conns[i] = new RvConnection(service, network, daemon);
            conns[i].setDescription(description);
            final Elements subjects = element.getChildElements(SUBJECT);
            for (int j =0, jmax = subjects.size(); j < jmax; ++j)
                conns[i].addSubject(subjects.get(j).getValue());
        }
        return conns;
    }

    private static void loadSubjectTree(Element parent) {
        final JTree tree = UIManager.INSTANCE.getSubjectExplorer();
        final Element subjectsRoot = parent.getFirstChildElement(SUBJECTS);
        if (subjectsRoot == null) return;
        final SubjectElement root = (SubjectElement) SubjectHierarchy.INSTANCE.getRoot();
        final Elements subjects = subjectsRoot.getChildElements(SUBJECT);
        for (int i = subjects.size(); i != 0;) {
            loadSubjectTreeElement(subjects.get(--i), root, tree);
        }
    }

    private static void loadSubjectTreeElement(Element xmlElement, SubjectElement parent, JTree tree) {
        final String name = xmlElement.getAttributeValue(SUBJECT_NAME);
        final boolean selected = getBoolean(xmlElement, SUBJECT_SELECTED, true);
        final SubjectElement node = SubjectHierarchy.INSTANCE.getSubjectElement(parent, name, selected);
        if (getBoolean(xmlElement, SUBJECT_EXPANDED, true))
            tree.expandPath(new TreePath(node.getPath()));
        final Elements children = xmlElement.getChildElements(SUBJECT);
        for (int i = children.size(); i != 0;) {
            loadSubjectTreeElement(children.get(--i), node, tree);
        }
    }

    private static void loadTypes(Element parent) {
        final Elements typeElts = parent.getChildElements(TYPE);
        RecordTypes types = RecordTypes.getInstance();
        // If there are types defined in the project then use them.
        // Otherwise add in the default set of types.
        if (typeElts.size() > 0)
            types.clear();
        else
            types.reset();
        for (int i = typeElts.size(); i != 0;) {
            try {
                final Element typeElt = typeElts.get(--i);
                final String name = getString(typeElt, TYPE_NAME);
                final Color colour = getColour(typeElt, COLOUR, Color.BLACK);
                if (name.equals(RecordTypes.DEFAULT.getName())) {
                    RecordTypes.DEFAULT.setColour(colour);
                } else {
                    final Element matcherElt = typeElt.getFirstChildElement(TYPE_MATCHER);
                    final String matcherId = getString(matcherElt, TYPE_MATCHER_NAME);
                    final String matcherValue = getString(matcherElt, TYPE_MATCHER_VALUE);
                    final RecordMatcher matcher = RecordMatcher.createMatcher(matcherId, matcherValue);
                    final RecordType type = types.createType(name, colour, matcher);
                    type.setSelected(getBoolean(typeElt, TYPE_SELECTED, true));
                }
            } catch (Exception ignored) {
                // Intentionally ignored.
            }
        }
    }

    /**
     * Resets the state of the subject explorer tree.
     * <p>
     * Whether there was a saved configuration or not, this method will always
     * reset the state of the subject explorer tree.
     */
    protected void postDeleteHook() {
        final JTree tree = UIManager.INSTANCE.getSubjectExplorer();
        final SubjectHierarchy model = (SubjectHierarchy) tree.getModel();
        // Collapse everything except the root node.
        for (int i = tree.getRowCount() - 1; i != 0; --i)
            tree.collapseRow(i);
        final Enumeration nodes = ((DefaultMutableTreeNode) model.getRoot()).breadthFirstEnumeration();
        while (nodes.hasMoreElements())
            ((SubjectElement) nodes.nextElement()).setSelected(true);
    }

    public static void storeConnections(Element parent) {
        RvConnection[] connections = (RvConnection[]) Connections.getInstance().toArray();
        for (int i = 0, imax = connections.length; i < imax; ++i) {
            final RvConnection connection = connections[i];
            final Element element = appendElement(parent, RV_CONNECTION);
            setString(element, RV_DESCRIPTION, connection.getDescription());
            setString(element, RV_SERVICE, connection.getService());
            setString(element, RV_NETWORK, connection.getNetwork());
            setString(element, RV_DAEMON, connection.getDaemon());
            for (final Iterator j = connection.getSubjects().iterator(); j.hasNext(); )
                setString(element, SUBJECT, (String) j.next());
        }
    }

    private static void storeSubjectTree(Element parent) {
        final JTree tree = UIManager.INSTANCE.getSubjectExplorer();
        final SubjectHierarchy model = (SubjectHierarchy) tree.getModel();
        final Element subjects = appendElement(parent, SUBJECTS);
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        for (final Enumeration e = root.children(); e.hasMoreElements(); ) {
            storeSubjectTreeElement(subjects, (SubjectElement) e.nextElement());
        }
    }

    private static void storeSubjectTreeElement(Element parent, SubjectElement node) {
        final JTree tree = UIManager.INSTANCE.getSubjectExplorer();
        final Element child = new Element(SUBJECT);
        final TreePath path = new TreePath(node.getPath());
        child.addAttribute(new Attribute(SUBJECT_NAME, node.getElementName()));
        setBoolean(child, SUBJECT_EXPANDED, tree.isExpanded(path));
        setBoolean(child, SUBJECT_SELECTED, node.isSelected());
        parent.appendChild(child);
        for (final Enumeration e = node.children(); e.hasMoreElements(); ) {
            storeSubjectTreeElement(child, (SubjectElement) e.nextElement());
        }
    }

    private static void storeTypes(Element parent) {
        RecordType[] types = RecordTypes.getInstance().getAllTypes();
        for (int i = 0, imax = types.length; i < imax; ++i) {
            RecordType type = types[i];
            final Element typeElt = appendElement(parent, TYPE);
            setString(typeElt, TYPE_NAME, type.getName());
            if (!RecordTypes.DEFAULT.equals(type)) {
                setBoolean(typeElt, TYPE_SELECTED, type.isSelected());
                final RecordMatcher matcher = type.getMatcher();
                final Element matcherElt = appendElement(typeElt, TYPE_MATCHER);
                setString(matcherElt, TYPE_MATCHER_NAME, matcher.getId());
                setString(matcherElt, TYPE_MATCHER_VALUE, matcher.getValue());
            }
            setColour(typeElt, COLOUR, type.getColour());
        }
    }

}
