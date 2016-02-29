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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.provision.ProvisionErrors;
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

    static File getAuthorImageDir(FSEnvironment env, String author, String sessionId) {
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
        return IoUtils.newFile(getAuthorImageDir(fsImage.getFSEnvironment(), author, fsImage.sessionId), BACKUP, FSEnvironment.getFSRelativePath(relativePath));
    }

    static AuthorImage loadAuthorImage(FSEnvironment env, String author, String sessionId) throws ProvisionException {
        File dir = getAuthorHistoryDir(env, author);
        if(!dir.exists()) {
            throw ProvisionErrors.unknownUnit(author);
        }
        dir = getLastUpdateDir(dir, sessionId);
        if(dir == null) {
            return null;
        }
        return new AuthorHistory(env, author).loadSession(dir.getName());
    }

    static List<String> listAuthors(FSEnvironment env, String sessionId) throws ProvisionException {
        final List<String> allAuthors = listAuthors(env);
        if(allAuthors.isEmpty()) {
            return Collections.emptyList();
        }
        if(allAuthors.size() == 1) {
            final File imagePath = getLastUpdateDir(getAuthorHistoryDir(env, allAuthors.get(0)), sessionId);
            if(imagePath == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(allAuthors.get(0));
        }
        final List<String> authors = new ArrayList<String>(allAuthors.size());
        for(String author : allAuthors) {
            final File imagePath = getLastUpdateDir(getAuthorHistoryDir(env, author), sessionId);
            if(imagePath != null) {
                authors.add(author);
            }
        }
        return authors;
    }

/*    static List<AuthorImage> loadAuthorImages(FSEnvironment env, String sessionId) throws ProvisionException {

        final List<String> authors = listAuthors(env);
        if(authors.isEmpty()) {
            return Collections.emptyList();
        }
        if(authors.size() == 1) {
            final File imagePath = getLastUpdateDir(getAuthorHistoryDir(env, authors.get(0)), sessionId);
            if(imagePath == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(new AuthorHistory(env, authors.get(0)).loadSession(imagePath.getName()));
        }
        final List<AuthorImage> images = new ArrayList<AuthorImage>(authors.size());
        for(String author : authors) {
            final File imagePath = getLastUpdateDir(getAuthorHistoryDir(env, author), sessionId);
            if(imagePath != null) {
                images.add(new AuthorHistory(env, author).loadSession(imagePath.getName()));
            }
        }
        return images;
    }
*/
    private static File getLastUpdateDir(File authorHistoryDir, String sessionId) throws ProvisionException {
        File sessionPath = new File(authorHistoryDir, sessionId);
        if(!sessionPath.exists()) {
            return null;
        }
        if(sessionPath.isFile()) {
            try {
                sessionId = FileUtils.readFile(sessionPath);
            } catch (IOException e) {
                throw ProvisionErrors.readError(sessionPath, e);
            }
            sessionPath = new File(authorHistoryDir, sessionId);
            if(!sessionPath.exists()) {
                throw ProvisionErrors.pathDoesNotExist(sessionPath);
            }
        }
        return sessionPath;
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

    AuthorImage newSession(String sessionId) {
        return new AuthorImage(this, author, sessionId);
    }

    AuthorImage loadSession(String sessionId) throws ProvisionException {
        return new AuthorImage(this, author, sessionId);
    }

    AuthorImage loadLast() throws ProvisionException {
        final String sessionId = getLastSessionId();
        if(sessionId == null) {
            return null;
        }
        return new AuthorImage(this, author, sessionId);
    }
}
