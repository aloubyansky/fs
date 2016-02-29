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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.HashUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class FSReadOnlyImage extends FSSession {

    protected final FSEnvironment fsEnv;
    protected Map<String, AuthorSession> authors = Collections.emptyMap();
    protected final PathsOwnership ownership;

    public FSReadOnlyImage(FSEnvironment env, String sessionId) {
        super(env, sessionId);
        this.fsEnv = env;
        ownership = PathsOwnership.getInstance(fsEnv.getHistoryDir());
    }

    protected FSEnvironment getFSEnvironment() {
        return fsEnv;
    }

    public String readContent(String relativePath) throws ProvisionException {
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
            throw ProvisionErrors.hashCalculationFailed(target, e);
        }
    }

    protected byte[] getHash(File target) throws IOException {
        if(!target.exists()) {
            return null;
        }
        return HashUtils.hashFile(target);
    }

    protected AuthorSession addAuthor(String author) {
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