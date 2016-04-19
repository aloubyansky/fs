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
import java.util.NoSuchElementException;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class RootPathNode extends PathNode {

    private static final String PATHS_REPO_DIR = "paths";

    private final File pathsRepoDir;
    private Map<String, PathNode> tasks = new HashMap<String, PathNode>();

    RootPathNode(File f, File historyDir) {
        super(null, f);
        this.pathsRepoDir = new File(historyDir, PATHS_REPO_DIR);
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

    PathNode getOrNew(String relativePath) {
        final String[] segments = relativePath.split("/");
        PathNode node = this;
        int i = 0;
        while(i < segments.length) {
            final String name = segments[i++];
            PathNode child = node.children.get(name);
            if(child == null) {
                node = newChild(node, name);
                while(i < segments.length) {
                    node = newChild(node, segments[i++]);
                }
                break;
            } else {
                node = child;
            }
        }
        return node;
    }

    boolean isOwnedBy(String path, String user) throws ProvisionException {
        PathNode node = tasks.get(path);
        if(node == null) {
            node = get(path);
        }
        if(node == null) {
            final PathOwnership pathOwnership = loadOwnership(pathsRepoDir, path);
            if(pathOwnership == null) {
                return false;
            }
            return pathOwnership.isOwnedBy(user);
        } else if(node.ownership == null) {
            node.ownership = loadOwnership(pathsRepoDir, path);
        }

        if(node.ownership == null) {
            return false;
        }
        return node.ownership.isOwnedBy(user);
    }

    boolean isOnlyOwner(String user, String path) throws ProvisionException {
        PathNode node = tasks.get(path);
        if(node == null) {
            node = get(path);
        }
        if(node == null) {
            final PathOwnership pathOwnership = loadOwnership(pathsRepoDir, path);
            if(pathOwnership == null) {
                return false;
            }
            return pathOwnership.isOnlyOwner(user);
        } else if(node.ownership == null) {
            node.ownership = loadOwnership(pathsRepoDir, path);
        }

        if(node.ownership == null) {
            return false;
        }
        return node.ownership.isOnlyOwner(user);
    }

    void grab(String user, String path) throws ProvisionException {
        PathNode node = tasks.get(path);
        if(node == null) {
            node = getOrNew(path);
        }
        if(node.ownership == null) {
            node.ownership = loadOwnership(pathsRepoDir, path);
            if (node.ownership == null) {
                node.ownership = new PathOwnership(user);
            } else {
                node.ownership.addUser(user);
            }
        } else {
            node.ownership.addUser(user);
        }
    }

    boolean giveUp(UserImage userImage, String path) throws ProvisionException {
        return giveUp(userImage, path, false);
    }

    boolean giveUp(UserImage userImage, String path, boolean safe) throws ProvisionException {
        boolean removed = false;

        PathNode node = tasks.get(path);
        if(node == null) {
            node = getOrNew(path);
        }
        if(node.ownership == null) {
            node.ownership = loadOwnership(pathsRepoDir, path);
        }
        if(node.ownership != null) {
            removed = node.ownership.removeUser(userImage.getUsername());
        }
        if(removed) {
            return node.ownership.isOwned();
        }
        if(safe) {
            return false;
        }
        throw ProvisionErrors.userDoesNotOwnTargetPath(userImage.getUsername(), path);
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

    void deleteDir(MutableUserImage userImage, String relativePath, DeleteTask task) throws ProvisionException {
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
            deleteChildren(target, userImage, relativePath);
            target.children = Collections.emptyMap();
        }
    }

    boolean delete(MutableUserImage userImage, String relativePath, DeleteTask task) throws ProvisionException {
        if(giveUp(userImage, relativePath)) {
            return false; // still owned
        }

        final String[] segments = relativePath.split("/");
        PathNode target = this;
        for(String name : segments) {
            PathNode child = target.children.get(name);
            if(child == null) {
                child = newChild(target, name);
            } else {
                if(child.isDeleted()) {
                    return true;
                }
            }
            target = child;
        }
        setTask(target, task);
        if(!target.children.isEmpty()) { // it shouldn't be called for dirs actually
            deleteChildren(target, userImage, relativePath);
            target.children = Collections.emptyMap();
        }
        return true;
    }

    void write(MutableUserImage userImage, String relativePath, ContentWriter task, boolean dir) throws ProvisionException {
        final String[] segments = relativePath.split("/");
        PathNode parent = this;
        int i = 0;
        while(i < segments.length - 1) {
            String name = segments[i++];
            PathNode child = parent.children.get(name);
            if(child == null) {
                child = newChild(parent, name);
            } else if(child.isDeleted()) {
                child.contentTask = null;
                deleteChildren(child, userImage, child.getPath());
            }
            parent = child;
        }
        PathNode target = parent.children.get(segments[i]);
        if(target == null) {
            target = newChild(parent, segments[i]);
        } else if(dir && target.isDeleted()) {
            deleteChildren(target, userImage, relativePath);
        }
        setTask(target, task);
        if(!dir) {
            grab(userImage.getUsername(), relativePath);
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

    private void deleteChildren(PathNode node, MutableUserImage userImage, String relativePath) throws ProvisionException {
        if(!node.children.isEmpty()) {
            for (PathNode child : node.children.values()) {
                if(!child.isDeleted()) {
                    userImage.delete(relativePath + '/' + child.f.getName());
                }
            }
        }
        final String[] fsChildren = node.f.list();
        if(fsChildren.length > 0) {
            for(String name : fsChildren) {
                if(!node.children.containsKey(name)) {
                    userImage.delete(relativePath + '/' + name);
                }
            }
        }
    }

    private void setTask(PathNode node, ContentTask task) {
        node.contentTask = task;
        final String path = node.getPath();
        PathNode taskNode = tasks.get(path);
        if(taskNode == null) {
            tasks.put(path, node);
        }
    }

    void schedulePersistence(MutableEnvImage fsImage) throws ProvisionException {
        for(PathNode node : tasks.values()) {
            node.schedulePersistence(fsImage, pathsRepoDir);
        }
    }

    Iterator<ContentTask> getTasks() {
        return new Iterator<ContentTask>() {
            final Iterator<PathNode> scheduledTasks = tasks.values().iterator();
            private PathNode pathTasks;
            @Override
            public boolean hasNext() {
                if(pathTasks != null) {
                    return true;
                }
                pathTasks = doNext();
                return pathTasks != null;
            }
            private PathNode doNext() {
                while(scheduledTasks.hasNext()) {
                    final PathNode pathTasks = scheduledTasks.next();
                    if(pathTasks.contentTask != null) {
                        return pathTasks;
                    }
                }
                return null;
            }
            @Override
            public ContentTask next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                ContentTask next = pathTasks.contentTask;
                pathTasks = null;
                return next;
            }};
    }

    void clear() {
        tasks.clear();
        children = Collections.emptyMap();
    }
}
