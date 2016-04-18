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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.HashUtils;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class RootPathNode extends PathNode {

    private static final String OWNERSHIPS_DIR_NAME = "ownership";
    private static final String EXTERNAL_USER_TRUE = "externalUser=true";

    private final File pathsRepoDir;
    private Map<String, PathNode> tasks = new HashMap<String, PathNode>();

    RootPathNode(File f, File historyDir) {
        super(null, f);
        this.pathsRepoDir = new File(historyDir, OWNERSHIPS_DIR_NAME);
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

    boolean giveUp(String user, String path) throws ProvisionException {
        return giveUp(user, path, false);
    }

    boolean giveUp(String user, String path, boolean safe) throws ProvisionException {
        boolean removed = false;

        PathNode node = tasks.get(path);
        if(node == null) {
            node = getOrNew(path);
        }
        if(node.ownership == null) {
            node.ownership = loadOwnership(pathsRepoDir, path);
        }
        if(node.ownership != null) {
            removed = node.ownership.removeUser(user);
        }
        if(removed) {
            return node.ownership.isOwned();
        }
        if(safe) {
            return false;
        }
        throw ProvisionErrors.userDoesNotOwnTargetPath(user, path);
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

    boolean delete(String user, String relativePath, DeleteTask task) throws ProvisionException {
        if(giveUp(user, relativePath)) {
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
        if(!target.children.isEmpty()) { // it shouldn't be called to dirs actually
            unsetChildTasks(target);
            target.children = Collections.emptyMap();
        }
        return true;
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
        } else if(dir && target.isDeleted()) {
            deleteChildren(target);
        }
        setTask(target, task);
        if(!dir) {
            grab(user, relativePath);
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
            } else if(child.contentTask != null) {
                unsetTask(child);
            }
        }
    }

    private void deleteChildren(PathNode node) {
        if(!node.children.isEmpty()) {
            for (PathNode child : node.children.values()) {
                if(child.contentTask == null || !child.contentTask.isDelete()) {
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
        node.contentTask = task;
        final String path = node.getPath();
        PathNode taskNode = tasks.get(path);
        if(taskNode == null) {
            tasks.put(path, node);
        }
    }

    private void unsetTask(PathNode node) {
        node.contentTask = null;
    }

    void schedulePersistence(MutableEnvImage fsImage) throws ProvisionException {
        for(Map.Entry<String, PathNode> entry : tasks.entrySet()) {
            final PathNode node = entry.getValue();
            final PathOwnership pathOwnership = node.ownership;
            if(pathOwnership == null) {
                continue;
            }
            String pathHash;
            try {
                pathHash = HashUtils.hashToHexString(entry.getKey());
            } catch (IOException e) {
                throw ProvisionErrors.hashCalculationFailed(entry.getKey(), e);
            }
            if(pathOwnership.isOwned()) {
                final StringBuilder writer = new StringBuilder();
                if(pathOwnership.isExternalUser()) {
                    writer.append(EXTERNAL_USER_TRUE).append(FileUtils.LS);
                }
                for(String user : pathOwnership.getUsers()) {
                    writer.append(user).append(FileUtils.LS);
                }
                fsImage.write(writer.toString(), new File(pathsRepoDir, pathHash));
            } else {
                fsImage.delete(new File(pathsRepoDir, pathHash));
            }
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

    static PathOwnership loadOwnership(File persistDir, String relativePath) throws ProvisionException {
        //final String[] steps = relativePath.split("/");
        //final File ownershipFile = IoUtils.newFile(persistDir, steps);
        File ownershipFile;
        try {
            ownershipFile = new File(persistDir, HashUtils.hashToHexString(relativePath));
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(relativePath, e);
        }
        if(!ownershipFile.exists()) {
            return null;
        }
        if(ownershipFile.isDirectory()) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(ownershipFile));
            String line = reader.readLine();
            if(line == null) {
                return null;
            }
            final PathOwnership ownership;
            if(EXTERNAL_USER_TRUE.equals(line)) {
                ownership = new PathOwnership(true);
            } else {
                ownership = new PathOwnership(line);
            }
            line = reader.readLine();
            while(line != null) {
                ownership.addUser(line);
                line = reader.readLine();
            }
            return ownership;
        } catch(IOException e) {
            throw ProvisionErrors.readError(ownershipFile, e);
        } finally {
            IoUtils.safeClose(reader);
        }
    }
}
