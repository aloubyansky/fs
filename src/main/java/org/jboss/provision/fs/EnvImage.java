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
import java.io.IOException;
import java.util.List;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.HashUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class EnvImage extends FSSession {

    protected final FSEnvironment fsEnv;
    protected final RootPathNode root;

    public EnvImage(FSEnvironment env, String sessionId) throws ProvisionException {
        super(env, sessionId);
        this.fsEnv = env;
        root = new RootPathNode(fsEnv.getHomeDir(), fsEnv.getHistoryDir());
    }

    protected FSEnvironment getFSEnvironment() {
        return fsEnv;
    }

    public UserImage getUserImage(String user) throws ProvisionException {
        return UserHistory.loadUserImage(fsEnv, user, sessionId);
    }

    public List<String> getUsers() throws ProvisionException {
        return UserHistory.listUsers(fsEnv, sessionId);
    }

    protected String readContent(String relativePath) throws ProvisionException {
        return readContent(fsEnv.getFile(relativePath));
    }

    protected String readContent(File target) throws ProvisionException {
        if (!target.exists()) {
            return null;
        }
        try {
            return FileUtils.readFile(target);
        } catch (IOException e) {
            throw ProvisionErrors.readError(target, e);
        }
    }

    public boolean contains(String relativePath) {
        return contains(fsEnv.getFile(relativePath));
    }

    protected boolean contains(File target) {
        return target.exists();
    }

    public byte[] getHash(String relativePath) throws ProvisionException {
        final File target = fsEnv.getFile(relativePath);
        try {
            return getHash(target);
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(relativePath, e);
        }
    }

    protected byte[] getHash(File target) throws ProvisionException, IOException {
        if(!target.exists()) {
            return null;
        }
        return HashUtils.hashFile(target);
    }

    protected boolean isOnlyOwner(String user, String relativePath) throws ProvisionException {
        return root.isOnlyOwner(user, relativePath);
    }

    protected void clear() {
        root.clear();
    }

    protected void undo(MutableEnvImage envImage) throws ProvisionException {

        System.out.println("EnvImage.undo " + sessionId);
        if(!sessionDir.exists()) {
            throw ProvisionErrors.pathDoesNotExist(sessionDir.getAbsoluteFile());
        }

        UserHistory.undo(envImage, sessionId);

        //final boolean lastCommit = sessionId.equals(fsEnv.getLastSessionId());

        final File deletedDir = new File(sessionDir, "d");
        if(deletedDir.exists()) {
            for(File deletedPath : deletedDir.listFiles()) {
                try {
                    final String content = FileUtils.readFile(deletedPath);
                    System.out.println("  d path=" + deletedPath.getName() + " content=" + content);
                    envImage.root.getByHash(deletedPath.getName(), "dir".equals(content)).scheduleUndo(envImage, sessionId, false);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        final File writeDir = new File(sessionDir, "w");
        if(writeDir.exists()) {
            for(File path : writeDir.listFiles()) {
                try {
                    final String content = FileUtils.readFile(path);
                    System.out.println("  w path=" + path.getName() + " content=" + content);
                    envImage.root.getByHash(path.getName(), "dir".equals(content)).scheduleUndo(envImage, sessionId, true);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        //root.scheduleUndo(this);
        super.scheduleDelete(envImage);
    }
}