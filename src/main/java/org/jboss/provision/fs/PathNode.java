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
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.HashUtils;
import org.jboss.provision.util.IoUtils;


/**
 *
 * @author Alexey Loubyansky
 */
class PathNode {

    private static final String EXTERNAL_USER_TRUE = "externalUser=true";
    private static final String LASTHASH = "lasthash";
    private static final String OWNERSHIP = "ownership";
    private static final String PATH = "path";
    private static final String SESSIONS = "sessions";

    protected static PathNode newPath(PathNode parent, String name, boolean dir) throws ProvisionException {
        final String relativePath;
        final File pathsDir;
        if(parent.parent == null) {
            relativePath = name;
            pathsDir = parent.nodeDir;
        } else {
            relativePath = parent.relativePath + '/' + name;
            pathsDir = parent.nodeDir.getParentFile();
        }
        return new PathNode(parent, relativePath, new File(parent.f, name), new File(pathsDir, getHash(relativePath)), dir);
    }

    private static PathOwnership loadOwnership(File ownershipFile) throws ProvisionException {
        if(!ownershipFile.exists()) {
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

    private static String getHash(String relativePath) throws ProvisionException {
        try {
            return HashUtils.hashToHexString(relativePath);
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(relativePath, e);
        }
    }

    protected static String getPathForHash(File pathsDir, String hash) throws ProvisionException {
        File pathFile = new File(pathsDir, hash);
        if(!pathFile.exists()) {
            throw ProvisionErrors.pathDoesNotExist(pathFile);
        }
        pathFile = new File(pathFile, PATH);
        if(!pathFile.exists()) {
            throw ProvisionErrors.pathDoesNotExist(pathFile);
        }
        try {
            return FileUtils.readFile(pathFile);
        } catch (IOException e) {
            throw ProvisionErrors.readError(pathFile, e);
        }
    }

    protected final PathNode parent;
    protected final String relativePath;
    protected final File f;
    protected final File nodeDir;
    protected final boolean dir;
    protected ContentTask contentTask;
    protected PathOwnership ownership;
    protected List<String> sessions;
    protected Map<String, PathNode> children = Collections.emptyMap();

    private String key;

    protected PathNode(PathNode parent, String relativePath, File f, File nodeDir, boolean dir) throws ProvisionException {
        this.parent = parent;
        this.relativePath = relativePath;
        this.f = f;
        this.dir = dir;
        this.nodeDir = nodeDir;
        this.key = nodeDir.getName();
    }

    protected ContentTask getContentTask() {
        return contentTask;
    }

    protected boolean exists() {
        if(contentTask == null) {
            return f.exists();
        }
        return !contentTask.isDelete();
    }

    protected boolean isDeleted() {
        return contentTask != null && contentTask.isDelete();
    }

    protected String getKey() throws ProvisionException {
        return key;
    }

    protected int getChildrenTotal() {
        if(!f.isDirectory()) {
            return 0;
        }
        final String[] actual = f.list();
        if(actual.length == 0) {
            int count = 0;
            for(PathNode child : children.values()) {
                if(child.contentTask != null && !child.contentTask.isDelete()) {
                    ++count;
                }
            }
            return count;
        }
        final Set<String> expected = new HashSet<String>(Arrays.asList(actual));
        for(PathNode child : children.values()) {
            if(child.getContentTask() != null) {
                if(child.getContentTask().isDelete()) {
                    expected.remove(child.f.getName());
                } else {
                    expected.add(child.f.getName());
                }
            }
        }
        return expected.size();
    }

    protected String getRelativePath() {
        return relativePath;
    }

    protected PathOwnership getOwnership() throws ProvisionException {
        if(ownership == null) {
            ownership = loadOwnership(new File(nodeDir, OWNERSHIP));
        }
        return ownership;
    }

    protected boolean isOwnedBy(String user) throws ProvisionException {
        final PathOwnership ownership = getOwnership();
        return ownership == null ? false : ownership.isOwnedBy(user);
    }

    protected boolean isOnlyOwner(String user) throws ProvisionException {
        final PathOwnership ownership = getOwnership();
        return ownership == null ? false : ownership.isOnlyOwner(user);
    }

    protected void addOwner(String user) throws ProvisionException {
        final PathOwnership ownership = getOwnership();
        if(ownership == null) {
            this.ownership = new PathOwnership(user);
        } else {
            ownership.addUser(user);
        }
    }

    protected boolean removeOwner(String user, boolean safe) throws ProvisionException {
        final PathOwnership ownership = getOwnership();
        if(ownership != null) {
            if(ownership.removeUser(user)) {
                return ownership.isOwned();
            }
        }
        if(safe) {
            return false;
        }
        throw ProvisionErrors.userDoesNotOwnTargetPath(user, relativePath);
    }

    protected void schedulePersistence(MutableEnvImage fsImage) throws ProvisionException {

        if(!nodeDir.exists()) {
            fsImage.write(relativePath, new File(nodeDir, PATH));
        }

        if(dir) {
            if(contentTask == null) {
                return;
            }
            try {
                fsImage.write("dir", IoUtils.newFile(fsImage.sessionDir,
                        contentTask.isDelete() ? "d" : "w", HashUtils.hashToHexString(relativePath)));
            } catch (IOException e) {
                throw ProvisionErrors.hashCalculationFailed(relativePath, e);
            }
        }

        if (ownership != null) {
            if (ownership.isOwned()) {
                final StringBuilder writer = new StringBuilder();
                if (ownership.isExternalUser()) {
                    writer.append(EXTERNAL_USER_TRUE).append(FileUtils.LS);
                }
                for (String user : ownership.getUsers()) {
                    writer.append(user).append(FileUtils.LS);
                }
                fsImage.write(writer.toString(), new File(nodeDir, OWNERSHIP));
            } else {
                fsImage.delete(new File(nodeDir, OWNERSHIP));
            }
        }

        if(contentTask != null) {
            final StringBuilder sessions = new StringBuilder(fsImage.sessionId);
            final File sessionsFile = new File(nodeDir, SESSIONS);
            if (sessionsFile.exists()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(sessionsFile));
                    String line = reader.readLine();
                    while (line != null) {
                        sessions.append(FileUtils.LS).append(line);
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    throw ProvisionErrors.readError(sessionsFile, e);
                } finally {
                    IoUtils.safeClose(reader);
                }
            }
            fsImage.write(sessions.toString(), sessionsFile);

            if(contentTask.canHashContent()) {
                final String currentHash;
                if(f.exists()) {
                    try {
                        currentHash = HashUtils.bytesToHexString(HashUtils.hashFile(f));
                    } catch (IOException e) {
                        throw ProvisionErrors.hashCalculationFailed(f.getAbsolutePath(), e);
                    }
                } else {
                    currentHash = null;
                }

                final File lastHashFile = new File(nodeDir, LASTHASH);
                if(lastHashFile.exists()) {
                    final String lastHash;
                    try {
                        lastHash = FileUtils.readFile(lastHashFile);
                    } catch (IOException e) {
                        throw ProvisionErrors.readError(lastHashFile, e);
                    }

                    if (!lastHash.equals(currentHash)) {
                        throw ProvisionErrors.pathHashMismatch(relativePath, lastHash, currentHash);
                    }
                    fsImage.write(f, IoUtils.newFile(nodeDir, currentHash));
                } else if(currentHash != null) {
                    fsImage.write(f, IoUtils.newFile(nodeDir, "preexisting"));
                }

                final String contentHash = HashUtils.bytesToHexString(contentTask.getContentHash());

                try {
                    if (contentTask.isDelete()) {
                        fsImage.write(contentHash,
                                IoUtils.newFile(fsImage.sessionDir, "d", HashUtils.hashToHexString(relativePath)));
                        fsImage.delete(new File(nodeDir, LASTHASH));
                    } else {
                        fsImage.write(contentHash,
                                IoUtils.newFile(fsImage.sessionDir, "w", HashUtils.hashToHexString(relativePath)));
                        fsImage.write(contentHash, lastHashFile);
                    }
                } catch (IOException e) {
                    throw ProvisionErrors.hashCalculationFailed(relativePath, e);
                }
            }

            fsImage.write(contentTask);
        }
    }

    protected void scheduleUndo(MutableEnvImage envImage, String sessionId, boolean write) throws ProvisionException {

        System.out.println("PathNode.scheduleUndo " + relativePath + " dir=" + dir + " write=" + write);

        final File sessionsFile = new File(nodeDir, SESSIONS);
        if(!sessionsFile.exists()) {
            throw ProvisionErrors.pathDoesNotExist(sessionsFile);
        }
        if (sessions == null) {
            try {
                sessions = FileUtils.readList(sessionsFile);
            } catch (IOException e) {
                throw ProvisionErrors.readError(sessionsFile, e);
            }
        }
        final int sessionInd = sessions.indexOf(sessionId);
        if(sessionInd < 0) {
            throw ProvisionErrors.sessionRecordMissingForPath(sessionId, relativePath);
        }

        if(sessions.size() == 1) {
            if(write) {
                if(dir) {
                    envImage.write(new DeleteTask(f, true));
                } else {
                    final File preexistingFile = new File(nodeDir, "preexisting");
                    if (preexistingFile.exists()) {
                        envImage.write(preexistingFile, f);
                    } else {
                        envImage.delete(f);
                    }
                }
            }
            envImage.delete(nodeDir);
        } else {
            if (ownership != null) {
                if (ownership.isOwned()) {
                    final StringBuilder writer = new StringBuilder();
                    if (ownership.isExternalUser()) {
                        writer.append(EXTERNAL_USER_TRUE).append(FileUtils.LS);
                    }
                    for (String user : ownership.getUsers()) {
                        writer.append(user).append(FileUtils.LS);
                    }
                    envImage.write(writer.toString(), new File(nodeDir, OWNERSHIP));
                } else {
                    envImage.delete(new File(nodeDir, OWNERSHIP));
                }
            }

            sessions.remove(sessionInd);
            final StringBuilder buf = new StringBuilder(sessions.get(0));
            for(int i = 1; i < sessions.size(); ++i) {
                buf.append(FileUtils.LS).append(sessions.get(i));
            }
            envImage.write(buf.toString(), sessionsFile);

            if(sessionInd == 0) {
                final String prevSession = sessions.get(0);
                final File prevSessionDir = new File(envImage.getFSEnvironment().getHistoryDir(), prevSession);
                File pathFile = IoUtils.newFile(prevSessionDir, "w", getKey());
                if(pathFile.exists()) {
                    if(dir) {
                        // nothing to do
                        envImage.mkdirs(f);
                    } else {
                        final String prevContent;
                        try {
                            prevContent = FileUtils.readFile(pathFile);
                        } catch (IOException e) {
                            throw ProvisionErrors.readError(pathFile, e);
                        }
                        envImage.write(prevContent, new File(nodeDir, LASTHASH));
                        final File backUp = new File(nodeDir, prevContent);
                        if (backUp.exists()) {
                            envImage.write(backUp, f);
                            envImage.delete(backUp);
                        }
                    }
                } else {
                    pathFile = IoUtils.newFile(prevSessionDir, "d", getKey());
                    if(pathFile.exists()) {
                        if(dir) {
                            envImage.write(new DeleteTask(f, true));
                        } else {
                            final File lastHash = new File(nodeDir, LASTHASH);
                            if (lastHash.exists()) {
                                envImage.delete(lastHash);
                            }
                        }
                    } else {
                        throw ProvisionErrors.sessionRecordMissingForPath(prevSession, relativePath);
                    }
                }
            } else {
                final File pathFile = IoUtils.newFile(envImage.getFSEnvironment().getHistoryDir(), sessionId, "w", getKey());
                if (pathFile.exists()) {
                    final String contentHash;
                    try {
                        contentHash = FileUtils.readFile(pathFile);
                    } catch (IOException e) {
                        throw ProvisionErrors.readError(pathFile, e);
                    }
                    final File contentFile = new File(nodeDir, contentHash);
                    if (contentFile.exists()) {
                        envImage.delete(contentFile);
                    }
                }
            }
        }
    }

    void logTree() {
        buildTree(this, System.out, new LinkedList<Boolean>());
    }
    private static void buildTree(PathNode node, PrintStream out, LinkedList<Boolean> depth) {
        if(!depth.isEmpty()) {
            for(int i = 0; i < depth.size() - 1; ++i) {
                if(depth.get(i)) {
                    out.print("|  ");
                } else {
                    out.print("   ");
                }
            }
            if(depth.getLast()) {
                out.print("|--");
            } else {
                out.print("`--");
            }
        }
        out.print(node.f.getName());
        if(node.contentTask != null) {
            out.print(" [");
            out.print(node.contentTask.isDelete() ? "deleted" : "written");
            out.println(']');
        } else {
            out.println();
        }
        if(!node.children.isEmpty()) {
            final Iterator<PathNode> i = node.children.values().iterator();
            while(i.hasNext()) {
                final PathNode c = i.next();
                depth.addLast(i.hasNext());
                buildTree(c, out, depth);
                depth.removeLast();
            }
        }
    }
}
