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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jboss.provision.ProvisionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class MutableUserImage extends UserImage {

    private final MutableEnvImage fsImage;
    private Map<String, Character> journal = Collections.emptyMap();

    MutableUserImage(UserHistory history, String author, MutableEnvImage fsImage) {
        super(history, author, fsImage.sessionId);
        this.fsImage = fsImage;
    }

    public MutableEnvImage getEnvImage() {
        return fsImage;
    }

    public MutableUserImage delete(String relativePath) throws ProvisionException {
        fsImage.delete(relativePath, username, true);
        return this;
    }

    public MutableUserImage write(String content, String relativePath) throws ProvisionException {
        fsImage.write(content, relativePath, username);
        return this;
    }

    public MutableUserImage write(File content, String relativePath) throws ProvisionException {
        fsImage.write(content, relativePath, username, true);
        return this;
    }

    public MutableUserImage mkdirs(String relativePath) throws ProvisionException {
        fsImage.mkdirs(relativePath, username);
        return this;
    }

    protected void addPath(String relativePath) throws ProvisionException {
        int i = relativePath.indexOf('/');
        while(i >= 0) {
            final String stepPath = relativePath.substring(0, i);
            if(fsImage.contains(stepPath)) {
                break;
            }
            putInJournal(stepPath, 'c');
            i = relativePath.indexOf('/', i + 1);
        }
        getPaths().add(relativePath);
        putInJournal(relativePath, 'c');
    }

    protected void removePath(String relativePath) throws ProvisionException {
        getPaths().remove(relativePath);
        putInJournal(relativePath, 'd');
    }

    protected void scheduleUnaffectedPersistence(MutableEnvImage fsImage) throws ProvisionException {
        fsImage.write(history.getLastSessionId(), sessionDir);
    }

    @Override
    protected void schedulePersistence(MutableEnvImage fsImage) throws ProvisionException {
        super.schedulePersistence(fsImage);
        fsImage.write(new ContentWriter(new File(sessionDir, TASKS)) {
            @Override
            public void write(BufferedWriter writer) throws IOException, ProvisionException {
                for(Map.Entry<String, Character> entry : journal.entrySet()) {
                    writer.write(entry.getValue());
                    writer.write(entry.getKey());
                    writer.newLine();
                }
            }
        });
    }

    private void putInJournal(String relativePath, char c) {
        switch(journal.size()) {
            case 0:
                journal = Collections.singletonMap(relativePath, c);
                break;
            case 1:
                if(journal.containsKey(relativePath)) {
                    journal = Collections.singletonMap(relativePath, c);
                    break;
                }
                journal = new HashMap<String, Character>(journal);
            default:
                journal.put(relativePath, c);
        }
    }
}
