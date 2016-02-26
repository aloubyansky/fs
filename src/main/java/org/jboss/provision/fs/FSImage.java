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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
public class FSImage extends FSSession{

    private static class OpDescr {
        ContentTask contentTask;
        OpDescr(ContentTask contentTask) {
            this.contentTask = contentTask;
        }
        void setTask(ContentTask contentTask) {
            this.contentTask = contentTask;
        }
    }

    private final FSEnvironment fsEnv;
    private Map<String, OpDescr> updates = new LinkedHashMap<String, OpDescr>();
    private Map<String, AuthorSession> authors = Collections.emptyMap();
    private final PathsOwnership ownership;

    FSImage(FSEnvironment fsEnv, String sessionId, PathsOwnership ownership) {
        super(fsEnv, sessionId);
        this.fsEnv = fsEnv;
        this.ownership = ownership;
    }

    FSImage(FSEnvironment fsEnv, String sessionId) {
        this(fsEnv, sessionId, null);
    }

    FSImage(FSEnvironment fsEnv) {
        this(fsEnv, UUID.randomUUID().toString(), PathsOwnership.getInstance(fsEnv.getHistoryDir()));
    }

    FSEnvironment getFSEnvironment() {
        return fsEnv;
    }

    FSImage write(ContentWriter contentWriter) throws ProvisionException {
        return write(contentWriter, null, null);
    }

    FSImage write(ContentWriter contentWriter, String relativePath, String author) throws ProvisionException {
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
        if(author != null) {
            ownership.addAuthor(relativePath, author);
            getAuthor(author).addPath(relativePath);
        }
        return this;
    }

    void delete(File target) throws ProvisionException {
        scheduleDelete(null, target, new DeleteTask(target), null);
    }

    public FSImage delete(String relativePath, String author) throws ProvisionException {
        final File target = fsEnv.getFile(relativePath);
        scheduleDelete(relativePath, target, new DeleteTask(target, AuthorHistory.getBackupPath(author, this, relativePath), false), author);
        return this;
    }

    protected void scheduleDelete(String relativePath, File target, ContentTask task, String author) throws ProvisionException {
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
                scheduleDelete(relativePath == null ? null : relativePath + '/' + f.getName(), f, DeleteTask.DELETE_FLAG, author);
            }
        } else {
            if(author != null) {
                if(!ownership.removeAuthor(relativePath, author)) {
                    throw ProvisionErrors.authorDoesNotOwnTargetPath(author, target.getAbsolutePath());
                }
                getAuthor(author).removePath(relativePath);
            }
        }
    }

    void write(String content, File target) throws ProvisionException {
        write(new StringContentWriter(content, target), null, null);
    }

    public FSImage write(String content, String relativePath, String author) throws ProvisionException {
        assert author != null : ProvisionErrors.nullArgument(author);
        write(new StringContentWriter(content, fsEnv.getFile(relativePath), AuthorHistory.getBackupPath(author, this, relativePath), false), relativePath, author);
        return this;
    }

    void write(File content, File target) throws ProvisionException {
        write(new FileContentWriter(content, target), null, null);
    }

    public FSImage write(File content, String relativePath, String author) throws ProvisionException {
        assert author != null : ProvisionErrors.nullArgument("author");
        write(new FileContentWriter(content, fsEnv.getFile(relativePath), AuthorHistory.getBackupPath(author, this, relativePath), false), relativePath, author);
        return this;
    }

    void mkdirs(File dir) throws ProvisionException {
        write(new MkDirsWriter(dir), null, null);
    }

    public String readContent(String relativePath) throws ProvisionException {
        return readContent(fsEnv.getFile(relativePath));
    }

    public String readContent(File target) throws ProvisionException {
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        if(opDescr == null) {
            if(!target.exists()) {
                return null;
            }
            try {
                return FileUtils.readFile(target);
            } catch (IOException e) {
                throw ProvisionErrors.readError(target, e);
            }
        }
        if(opDescr.contentTask.isDelete()) {
            return null;
        }
        return opDescr.contentTask.getContentString();
    }

    public boolean exists(String relativePath) {
        final File target = fsEnv.getFile(relativePath);
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        if(opDescr == null) {
            return target.exists();
        }
        return !opDescr.contentTask.isDelete();
    }

    public boolean isDeleted(String relativePath) {
        return isDeleted(fsEnv.getFile(relativePath));
    }

    public boolean isDeleted(File target) {
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        if(opDescr == null) {
            return false;
        }
        return opDescr.contentTask.isDelete();
    }

    public byte[] getHash(String relativePath) throws ProvisionException {
        final File target = fsEnv.getFile(relativePath);
        final OpDescr opDescr = updates.get(target.getAbsolutePath());
        try {
            if (opDescr == null) {
                if (!target.exists()) {
                    return null;
                }
                return HashUtils.hashFile(target);
            }
            if (opDescr.contentTask.isDelete()) {
                return null;
            }
            if (opDescr.contentTask.getContentFile() != null) {
                return HashUtils.hashFile(opDescr.contentTask.getContentFile());
            }
            if (opDescr.contentTask.getContentString() != null) {
                return HashUtils.hashBytes(opDescr.contentTask.getContentString().getBytes());
            }
            if (!opDescr.contentTask.getTarget().exists()) {
                return null;
            }
            return HashUtils.hashFile(opDescr.contentTask.getTarget());
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(target, e);
        }
    }

    void schedulePersistence() throws ProvisionException {
        schedulePersistence(this);
        final Set<String> notAffectedAuthors = new HashSet<String>(AuthorHistory.listAuthors(fsEnv));
        for(AuthorSession author : authors.values()) {
            notAffectedAuthors.remove(author.getName());
            author.schedulePersistence(this);
        }
        if(!notAffectedAuthors.isEmpty()) {
            for(String author : notAffectedAuthors) {
                new AuthorHistory(fsEnv, author).newSession(sessionId).scheduleUnaffectedPersistence(this);
            }
        }
        ownership.schedulePersistence(this);
    }

    public void commit() throws ProvisionException {

        schedulePersistence();

        final OpDescr[] ops = new OpDescr[updates.size()];
        int i = 0;

        // backup
        try {
            for (OpDescr op : updates.values()) {
                ops[i++] = op;
                op.contentTask.backup();
            }
        } catch (ProvisionException | RuntimeException | Error e) {
            while(i > 0) {
                try {
                    ops[--i].contentTask.cleanup();
                } catch (ProvisionException e1) {
                    e1.printStackTrace();
                }
            }
            throw ProvisionErrors.backupFailed(e);
        }

        // execute
        try {
            i = 0;
            while(i < ops.length) {
                ops[i++].contentTask.execute();
            }
        } catch (ProvisionException | RuntimeException | Error e) {
            while (i > 0) {
                try {
                    ops[--i].contentTask.revert();
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            throw ProvisionErrors.failedToCopyContent(e);
        }

        // cleanup
        while(i > 0) {
            try {
                ops[--i].contentTask.cleanup();
            } catch (ProvisionException | RuntimeException | Error e) {
                e.printStackTrace();
            }
        }

        updates.clear();
        authors = Collections.emptyMap();
        ownership.clear();
    }

    public boolean isUntouched() {
        return updates.isEmpty();
    }

    public void logUpdates(PrintStream out) {
        for(OpDescr op : updates.values()) {
            out.println(op.contentTask);
        }
    }

    private AuthorSession getAuthor(String author) {
        AuthorSession session = authors.get(author);
        if(session != null) {
            return session;
        }
        session = new AuthorHistory(fsEnv, author).newSession(sessionId);
        switch(authors.size()) {
            case 0:
                authors = Collections.singletonMap(author, session);
                break;
            case 1:
                authors = new HashMap<String, AuthorSession>(authors);
            default:
                authors.put(author, session);
        }
        return session;
    }
}