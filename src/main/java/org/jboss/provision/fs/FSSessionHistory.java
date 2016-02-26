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

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
abstract class FSSessionHistory {

    protected static final String LAST_SESSION_TXT = "last.txt";
    protected static final String NEXT_SESSION_TXT = "next.txt";
    protected static final String PREV_SESSION_TXT = "prev.txt";

    protected static File getFileToPersist(final File dir, String name) throws ProvisionException {
        final File f = new File(dir, name);
        if(f.exists()) {
            throw ProvisionErrors.pathAlreadyExists(f);
        }
        return f;
    }

    protected static File getFileToLoad(final File instrDir, String name) throws ProvisionException {
        final File f = new File(instrDir, name);
        if(!f.exists()) {
            throw ProvisionErrors.pathDoesNotExist(f);
        }
        return f;
    }

    protected final File historyDir;
    private String lastSessionId;

    protected FSSessionHistory(File historyDir) {
        assert historyDir != null : ProvisionErrors.nullArgument("historyDir");
        this.historyDir = historyDir;
    }

    File getHistoryDir() {
        return historyDir;
    }

    File getLastSessionDir() throws ProvisionException {
        final String lastId = getLastSessionId();
        if(lastId == null) {
            return null;
        }
        return new File(historyDir, lastId);
    }

    String getLastSessionId() throws ProvisionException {
        if(lastSessionId != null) {
            return lastSessionId;
        }
        final File lastTxt = new File(historyDir, LAST_SESSION_TXT);
        if(!lastTxt.exists()) {
            return null;
        }
        try {
            lastSessionId = FileUtils.readFile(lastTxt);
        } catch (IOException e) {
            throw ProvisionErrors.readError(IoUtils.newFile(historyDir, LAST_SESSION_TXT), e);
        }
        return lastSessionId;
    }
}
