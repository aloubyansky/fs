/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.provision.fs;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class RootPathNode extends PathNode {

    private final PathsOwnership ownership;
    private Map<String, ContentTask> tasks = new HashMap<String, ContentTask>();

    RootPathNode(File f, PathsOwnership ownership) {
        super(null, f);
        this.ownership = ownership;
    }

    PathNode get(String relativePath) {
        final String[] segments = relativePath.split("/");
        PathNode node = this;
        for(String name : segments) {
            node = node.children.get(name);
            if(node == null) {
                return null;
            }
        }
        return node;
    }

    boolean isDeleted(String relativePath) {
        final String[] segments = relativePath.split("/");
        PathNode node = this;
        for(String name : segments) {
            node = node.children.get(name);
            if(node == null) {
                return false;
            }
            if(node.isDeleted()) {
                return true;
            }
        }
        return false;
    }

    void deleteDir(String relativePath, DeleteTask task) throws ProvisionException {
        final String[] segments = relativePath.split("/");
        PathNode target = this;
        for(String name : segments) {
            PathNode child = target.children.get(name);
            if(child == null) {
                child = newChild(target, name);
            } else {
                if(child.isDeleted()) {
                    return;
                }
            }
            target = child;
        }
        setTask(target, task);
        if(!target.children.isEmpty()) {
            unsetChildTasks(target);
            target.children = Collections.emptyMap();
        }
    }

    void delete(String user, String relativePath, DeleteTask task) throws ProvisionException {
        if(!ownership.giveUp(user, relativePath)) {
            throw ProvisionErrors.userDoesNotOwnTargetPath(user, relativePath);
        }
        final String[] segments = relativePath.split("/");
        PathNode target = this;
        for(String name : segments) {
            PathNode child = target.children.get(name);
            if(child == null) {
                child = newChild(target, name);
            } else {
                if(child.isDeleted()) {
                    return;
                }
            }
            target = child;
        }
        setTask(target, task);
        if(!target.children.isEmpty()) {
            unsetChildTasks(target);
            target.children = Collections.emptyMap();
        }
    }

    void write(String user, String relativePath, ContentWriter task, boolean dir) throws ProvisionException {
        final String[] segments = relativePath.split("/");
        PathNode parent = this;
        int i = 0;
        while(i < segments.length - 1) {
            String name = segments[i++];
            PathNode child = parent.children.get(name);
            if(child == null) {
                child = newChild(parent, name);
            } else if(child.isDeleted()) {
                unsetTask(child);
                deleteChildren(child);
            }
            parent = child;
        }
        PathNode target = parent.children.get(segments[i]);
        if(target == null) {
            target = newChild(parent, segments[i]);
        }
        setTask(target, task);
        if(!dir) {
            ownership.grab(user, relativePath);
        }
    }

    private static PathNode newChild(PathNode parent, String name) {
        final PathNode leaf = new PathNode(parent, new File(parent.f, name));
        switch(parent.children.size()) {
            case 0:
                parent.children = Collections.singletonMap(name, leaf);
                break;
            case 1:
                parent.children = new HashMap<String, PathNode>(parent.children);
            default:
                parent.children.put(name, leaf);
        }
        return leaf;
    }

    private void unsetChildTasks(PathNode node) {
        for(PathNode child : node.children.values()) {
            if(!child.children.isEmpty()) {
                unsetChildTasks(child);
            } else if(child.task != null) {
                unsetTask(child);
            }
        }
    }

    private void deleteChildren(PathNode node) {
        if(!node.children.isEmpty()) {
            for (PathNode child : node.children.values()) {
                if(child.task == null || !child.task.isDelete()) {
                    setTask(child, new DeleteTask(child.f)); // TODO proper backup
                }
            }
        }
        final String[] fsChildren = node.f.list();
        if(fsChildren.length > 0) {
            for(String name : fsChildren) {
                if(!node.children.containsKey(name)) {
                    setTask(newChild(node, name), new DeleteTask(new File(node.f, name))); // TODO proper backup
                }
            }
        }
    }

    private void setTask(PathNode node, ContentTask task) {
        node.task = task;
        tasks.put(node.getPath(), task);
    }

    private void unsetTask(PathNode node) {
        node.task = null;
        tasks.remove(node.getPath());
    }

    Iterator<ContentTask> getTasks() {
        return tasks.values().iterator();
    }
}
