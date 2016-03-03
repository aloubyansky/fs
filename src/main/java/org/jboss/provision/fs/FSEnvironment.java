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
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FSEnvironment extends FSSessionHistory {

    public static FSEnvironment create(FSEnvironmentConfig config) {
        return new FSEnvironment(config);
    }

    private final File homeDir;

    private FSEnvironment(FSEnvironmentConfig config) {
        super(config.historyDir);
        this.homeDir = config.homeDir;
    }

    public File getHomeDir() {
        return homeDir;
    }

    @Override
    File getHistoryDir() {
        return historyDir;
    }

    public static String getFSRelativePath(String relativePath) {
        if(relativePath == null) {
            return null;
        }
        return File.separatorChar == '\\' ? relativePath.replace('/', '\\') : relativePath;
    }

    public File getFile(String relativePath) {
        return new File(homeDir, getFSRelativePath(relativePath));
    }

    public MutableEnvImage newImage() {
        return new MutableEnvImage(this);
    }

    protected EnvImage getImage(String id) throws ProvisionException {
        return new EnvImage(this, id);
    }

    public EnvImage getImage() throws ProvisionException {
        final String sessionId = getLastSessionId();
        if(sessionId == null) {
            return null;
        }
        return getImage(sessionId);
    }

    public Iterator<EnvImage> envHistory() throws ProvisionException {
        return new ImageIterator<EnvImage>(getImage()) {
            @Override
            protected EnvImage getPrevious(EnvImage image) throws ProvisionException {
                final String prevId = image.getPreviousRecordId();
                if(prevId == null) {
                    return null;
                }
                return getImage(prevId);
            }
        };
    }

    public Iterator<UserImage> userHistory(String username) throws ProvisionException {
        final UserHistory userHistory = new UserHistory(this, username);
        return new ImageIterator<UserImage>(userHistory.loadLatest()) {
            @Override
            protected UserImage getPrevious(UserImage image) throws ProvisionException {
                final String prevId = image.getPreviousRecordId();
                if(prevId == null) {
                    return null;
                }
                return userHistory.loadImage(prevId);
            }
        };
    }

    public void undoLastCommit() throws ProvisionException {
        final String lastImageId = getLastSessionId();
        if(lastImageId == null) {
            throw ProvisionErrors.noHistoryRecordedUntilThisPoint();
        }
        final MutableEnvImage image = new MutableEnvImage(this, lastImageId);
        image.scheduleDelete();
        image.executeUpdates();
    }

    private abstract class ImageIterator<T> implements Iterator<T> {
        boolean doNext;
        private T image;
        ImageIterator(T first) {
            this.image = first;
        }
        @Override
        public boolean hasNext() {
            if(doNext) {
                doNext();
            }
            return image != null;
        }
        @Override
        public T next() {
            if(hasNext()) {
                doNext = true;
                return image;
            }
            throw new NoSuchElementException();
        }
        private void doNext() {
            if(!doNext) {
                return;
            }
            try {
                image = getPrevious(image);
            } catch(ProvisionException e) {
                throw new IllegalStateException(e);
            }
            doNext = false;
        }
        protected abstract T getPrevious(T image) throws ProvisionException;
    }
}
