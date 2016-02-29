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
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class AuthorHistory extends FSSessionHistory {

    private static final String AUTHORS_DIR_NAME = "authors";
    private static final String BACKUP = "backup";


    private static File getAuthorsDir(FSEnvironment env) {
        return new File(env.getHistoryDir(), AUTHORS_DIR_NAME);
    }

    private static File getAuthorHistoryDir(FSEnvironment env, String author) {
        return new File(getAuthorsDir(env), author);
    }

    static File getAuthorSessionDir(FSEnvironment env, String author, String sessionId) {
        return new File(getAuthorHistoryDir(env, author), sessionId);
    }

    static List<String> listAuthors(FSEnvironment env) {
        final File authorsDir = getAuthorsDir(env);
        if(!authorsDir.exists()) {
            return Collections.emptyList();
        }
        return Arrays.asList(authorsDir.list());
    }

    static File getBackupPath(String author, FSReadOnlyImage fsImage, String relativePath) {
        return IoUtils.newFile(getAuthorSessionDir(fsImage.getFSEnvironment(), author, fsImage.sessionId), BACKUP, FSEnvironment.getFSRelativePath(relativePath));
    }

    private final FSEnvironment env;
    private final String author;

    protected AuthorHistory(FSEnvironment env, String author) {
        super(getAuthorHistoryDir(env, author));
        this.env = env;
        this.author = author;
    }

    List<String> getSessionIds() {
        return Arrays.asList(historyDir.list(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }}));
    }

    AuthorSession newSession(String sessionId) {
        return new AuthorSession(this, author, sessionId);
    }

    AuthorSession loadSession(String sessionId) throws ProvisionException {
        return new AuthorSession(this, author, sessionId);
    }

    AuthorSession loadLast() throws ProvisionException {
        final String sessionId = this.getLastSessionId();
        if(sessionId == null) {
            return null;
        }
        return loadSession(sessionId);
    }
}
