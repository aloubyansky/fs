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
abstract class FSSession {

    protected final FSSessionHistory history;
    protected final String sessionId;
    protected final File sessionDir;

    protected FSSession(FSSessionHistory history, String sessionId) {
        this.history = history;
        this.sessionId = sessionId;
        this.sessionDir = new File(history.getHistoryDir(), sessionId);
    }

    File getSessionDir() {
        return sessionDir;
    }

    String getPreviousRecordId() throws ProvisionException {
        final File prevSessionTxt = new File(getSessionDir(), FSSessionHistory.PREV_SESSION_TXT);
        if(!prevSessionTxt.exists()) {
            return null;
        }
        try {
            return FileUtils.readFile(prevSessionTxt);
        } catch (IOException e) {
            throw ProvisionErrors.readError(prevSessionTxt, e);
        }
    }

    String getNextSessionId() throws ProvisionException {
        final File nextSessionTxt = new File(getSessionDir(), FSSessionHistory.NEXT_SESSION_TXT);
        if(!nextSessionTxt.exists()) {
            return null;
        }
        try {
            return FileUtils.readFile(nextSessionTxt);
        } catch (IOException e) {
            throw ProvisionErrors.readError(nextSessionTxt, e);
        }
    }

    protected void schedulePersistence(MutableEnvImage fsImage) throws ProvisionException {

        if (sessionDir.exists()) {
            if (!sessionDir.isDirectory()) {
                throw new ProvisionException(ProvisionErrors.notADir(sessionDir));
            }
        } else {
            fsImage.mkdirs(sessionDir);
        }

        final File prevRecordTxt = FSSessionHistory.getFileToPersist(sessionDir, FSSessionHistory.PREV_SESSION_TXT);
        final File lastRecordTxt = new File(history.getHistoryDir(), FSSessionHistory.LAST_SESSION_TXT);
        final File lastSessionDir = history.getLastSessionDir();

        if (lastSessionDir != null && !fsImage.isDeleted(lastSessionDir)) {
            fsImage.write(lastSessionDir.getName(), prevRecordTxt);
            final File nextInstrTxt = FSSessionHistory.getFileToPersist(lastSessionDir, FSSessionHistory.NEXT_SESSION_TXT);
            fsImage.write(sessionId, nextInstrTxt);
        }
        fsImage.write(sessionId, lastRecordTxt);
    }

    protected void scheduleDelete(MutableEnvImage fsImage) throws ProvisionException {
        if (!sessionDir.exists()) {
            return;
        }

        final String prevSessionId = fsImage.readContent(new File(sessionDir, FSSessionHistory.PREV_SESSION_TXT));
        final String nextSessionId = fsImage.readContent(new File(sessionDir, FSSessionHistory.NEXT_SESSION_TXT));

        if (prevSessionId != null) {
            final File nextSessionTxt = IoUtils.newFile(history.getHistoryDir(), prevSessionId, FSSessionHistory.NEXT_SESSION_TXT);
            if (nextSessionId != null) {
                if (nextSessionTxt.exists()) {
                    fsImage.write(nextSessionId, nextSessionTxt);
                } else {
                    throw new IllegalStateException("next record must exist");
                }
            } else {
                fsImage.delete(nextSessionTxt);
            }
        }
        if (nextSessionId != null) {
            final File prevSessionTxt = IoUtils.newFile(sessionDir, nextSessionId, FSSessionHistory.PREV_SESSION_TXT);
            if (prevSessionId != null) {
                if (prevSessionTxt.exists()) {
                    fsImage.write(prevSessionId, prevSessionTxt);
                } else {
                    throw new IllegalStateException("previous record must exist");
                }
            } else {
                fsImage.delete(prevSessionTxt);
            }
        }

        final File lastSessionTxt = new File(history.getHistoryDir(), FSSessionHistory.LAST_SESSION_TXT);
        final String lastSessionId = fsImage.readContent(lastSessionTxt);
        if (lastSessionId != null && lastSessionId.equals(sessionId)) {
            if (nextSessionId != null) {
                fsImage.write(nextSessionId, lastSessionTxt);
            } else if (prevSessionId != null) {
                fsImage.write(prevSessionId, lastSessionTxt);
            } else {
                fsImage.delete(lastSessionTxt);
            }
        }
        fsImage.delete(sessionDir);
    }
}