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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.HashUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class MutableEnvImage extends EnvImage {

    static class OpDescr {
        ContentTask contentTask;
        OpDescr(ContentTask contentTask) {
            this.contentTask = contentTask;
        }
        void setTask(ContentTask contentTask) {
            this.contentTask = contentTask;
        }
    }

    private final RootPathNode root;
    private Map<String, OpDescr> updates = new LinkedHashMap<String, OpDescr>();
    private Map<String, MutableUserImage> users = Collections.emptyMap();

    MutableEnvImage(FSEnvironment fsEnv, String sessionId) {
        super(fsEnv, sessionId);
        root = new RootPathNode(fsEnv.getHomeDir(), ownership);
    }

    MutableEnvImage(FSEnvironment fsEnv) {
        super(fsEnv, UUID.randomUUID().toString());
        root = new RootPathNode(fsEnv.getHomeDir(), ownership);
    }

    @Override
    public List<String> getUsers() throws ProvisionException {
        // TODO this method has to include new users participating in this image too
        return super.getUsers();
    }

    @Override
    public MutableUserImage getUserImage(String user) throws ProvisionException {
        MutableUserImage image = users.get(user);
        if(image != null) {
            return image;
        }
        image = new UserHistory(fsEnv, user).newImage(this);
        switch(users.size()) {
            case 0:
                users = Collections.singletonMap(user, image);
                break;
            case 1:
                users = new HashMap<String, MutableUserImage>(users);
            default:
                users.put(user, image);
        }
        return image;
    }

    @Override
    public String readContent(String relativePath) throws ProvisionException {
        final PathNode node = root.get(relativePath);
        if(node == null) {
            return super.readContent(relativePath);
        }
        return readContent(node.getTask());
    }

    @Override
    protected String readContent(File target) throws ProvisionException {
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        if(opDescr == null) {
            return super.readContent(target);
        }
        return readContent(opDescr.contentTask);
    }

    private String readContent(ContentTask contentTask) {
        if(contentTask.isDelete()) {
            return null;
        }
        return contentTask.getContentString();
    }

    @Override
    public boolean contains(String relativePath) {
        final PathNode node = root.get(relativePath);
        if(node == null) {
            return super.contains(relativePath);
        }
        return !node.isDeleted();
    }

    @Override
    protected boolean contains(File target) {
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        if(opDescr == null) {
            return target.exists();
        }
        return !opDescr.contentTask.isDelete();
    }

    @Override
    public byte[] getHash(String relativePath) throws ProvisionException {
        final PathNode node = root.get(relativePath);
        if(node == null) {
            return super.getHash(relativePath);
        }
        try {
            return getHash(node.getTask());
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(relativePath, e);
        }
    }

    @Override
    protected byte[] getHash(File target) throws IOException {
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        if (opDescr == null) {
            return super.getHash(target);
        }
        return getHash(opDescr.contentTask);
    }

    private byte[] getHash(ContentTask contentTask) throws IOException {
        if (contentTask.isDelete()) {
            return null;
        }
        if (contentTask.getContentFile() != null) {
            return HashUtils.hashFile(contentTask.getContentFile());
        }
        if (contentTask.getContentString() != null) {
            return HashUtils.hashBytes(contentTask.getContentString().getBytes());
        }
        if (!contentTask.getTarget().exists()) {
            return null;
        }
        return HashUtils.hashFile(contentTask.getTarget());
    }

    protected MutableEnvImage write(ContentWriter contentWriter) throws ProvisionException {
        final String targetPath = contentWriter.getTarget().getAbsolutePath();
        final OpDescr descr = updates.get(targetPath);
        if(descr != null) {
            if(descr.contentTask.isDelete()) {
                // re-schedule to be the last
                updates.remove(targetPath);
                updates.put(targetPath, descr);
            }
            descr.setTask(contentWriter);
        } else {
            updates.put(targetPath, new OpDescr(contentWriter));
        }
        return this;
    }

    protected MutableEnvImage write(ContentWriter contentWriter, String relativePath, String user, boolean dir) throws ProvisionException {
        getUserImage(user).addPath(relativePath, dir);
        root.write(user, relativePath, contentWriter, dir);
        return this;
    }

    protected void delete(File target) throws ProvisionException {
        scheduleDelete(null, target, new DeleteTask(target));
    }

    protected void scheduleDelete(String relativePath, File target, ContentTask task) throws ProvisionException {
        final OpDescr descr = updates.get(target.getAbsolutePath());
        if (descr != null) {
            if (descr.contentTask == DeleteTask.DELETE_FLAG) {
                return;
            }
            descr.setTask(task);
        } else {
            updates.put(target.getAbsolutePath(), new OpDescr(task));
        }
        if(target.isDirectory()) {
            for(File f : target.listFiles()) {
                scheduleDelete(relativePath == null ? null : relativePath + '/' + f.getName(), f, DeleteTask.DELETE_FLAG);
            }
        }
    }

    protected MutableEnvImage delete(String relativePath, String user, boolean backupForHistory) throws ProvisionException {
        final File target = fsEnv.getFile(relativePath);
        if(!target.exists()) {
            giveUp(target, relativePath, user);
            return this;
        }
        final DeleteTask task = backupForHistory ?
                new DeleteTask(target, UserHistory.getBackupPath(user, this, relativePath), false) :
                    new DeleteTask(target);
        if(target.isDirectory()) {
            root.deleteDir(relativePath, task);
            giveUp(target, relativePath, user);
        } else {
            root.delete(user, relativePath, task);
            getUserImage(user).removePath(relativePath);
        }
        return this;
    }

    protected void giveUp(File target, String relativePath, String user) throws ProvisionException {
        if(target.isDirectory()) {
            for(File child : target.listFiles()) {
                giveUp(child, relativePath + '/' + child.getName(), user);
            }
        } else {
            getUserImage(user).removePath(relativePath);
            ownership.giveUp(user, relativePath);
        }
    }

    protected void write(String content, File target) throws ProvisionException {
        write(new StringContentWriter(content, target));
    }

    protected MutableEnvImage write(String content, String relativePath, String user) throws ProvisionException {
        assert user != null : ProvisionErrors.nullArgument(user);
        write(new StringContentWriter(content, fsEnv.getFile(relativePath), UserHistory.getBackupPath(user, this, relativePath), false), relativePath, user, false);
        return this;
    }

    protected MutableEnvImage write(File content, String relativePath, String user, boolean backupForHistory) throws ProvisionException {
        assert user != null : ProvisionErrors.nullArgument("user");
        if(backupForHistory) {
            write(new CopyFileContentWriter(content, fsEnv.getFile(relativePath), UserHistory.getBackupPath(user, this, relativePath), false), relativePath, user, content.isDirectory());
        } else {
            write(new CopyFileContentWriter(content, fsEnv.getFile(relativePath)), relativePath, user, content.isDirectory());
        }
        return this;
    }

    protected MutableEnvImage mkdirs(String relativePath, String user) throws ProvisionException {
        write(new MkDirsWriter(fsEnv.getFile(relativePath)), relativePath, user, true);
        return this;
    }

    protected void mkdirs(File dir) throws ProvisionException {
        write(new MkDirsWriter(dir));
    }

    public boolean isDeleted(String relativePath) {
        return root.isDeleted(relativePath);
    }

    protected boolean isDeleted(File target) {
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        if(opDescr == null) {
            return false;
        }
        return opDescr.contentTask.isDelete();
    }

    protected void schedulePersistence() throws ProvisionException {
        schedulePersistence(this);
        final Set<String> notAffectedUsers = new HashSet<String>(UserHistory.listUsers(fsEnv));
        for(UserImage user : users.values()) {
            notAffectedUsers.remove(user.getUsername());
            user.schedulePersistence(this);
        }
        if(!notAffectedUsers.isEmpty()) {
            for(String user : notAffectedUsers) {
                new UserHistory(fsEnv, user).newImage(this).scheduleUnaffectedPersistence(this);
            }
        }
        ownership.schedulePersistence(this);
    }

    protected void scheduleDelete() throws ProvisionException {
        UserHistory.scheduleDelete(this, sessionId);
        ownership.schedulePersistence(this);
        super.scheduleDelete(this);
    }

    public void commit() throws ProvisionException {
        schedulePersistence();
        executeUpdates();
    }

    protected void executeUpdates() throws ProvisionException {

        root.logTree();

        final List<ContentTask> ops = new ArrayList<ContentTask>(updates.size());
        int i = 0;

        // backup
        try {
            final Iterator<ContentTask> tasks = root.getTasks();
            while(tasks.hasNext()) {
                ContentTask next = tasks.next();
                ++i;
                ops.add(next);
                next.backup();
            }
            for (OpDescr op : updates.values()) {
                ++i;
                ops.add(op.contentTask);
                op.contentTask.backup();
            }
        } catch (ProvisionException | RuntimeException | Error e) {
            while(i > 0) {
                try {
                    ops.get(--i).cleanup();
                } catch (ProvisionException e1) {
                    e1.printStackTrace();
                }
            }
            throw ProvisionErrors.backupFailed(e);
        }

        // execute
        try {
            i = 0;
            while(i < ops.size()) {
                ops.get(i++).execute();
            }
        } catch (ProvisionException | RuntimeException | Error e) {
            while (i > 0) {
                try {
                    ops.get(--i).revert();
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            throw ProvisionErrors.failedToCopyContent(e);
        }

        // cleanup
        while(i > 0) {
            try {
                ops.get(--i).cleanup();
            } catch (ProvisionException | RuntimeException | Error e) {
                e.printStackTrace();
            }
        }

        clear();
    }

    public boolean isUntouched() {
        return updates.isEmpty();
    }

    public void logUpdates(PrintStream out) {
        for(OpDescr op : updates.values()) {
            out.println(op.contentTask);
        }
    }

    @Override
    protected void clear() {
        super.clear();
        users = Collections.emptyMap();
        updates.clear();
    }
}