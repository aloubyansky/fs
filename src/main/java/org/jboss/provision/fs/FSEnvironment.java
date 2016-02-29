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

    public FSImage newImage() {
        return new FSImage(this);
    }

    public FSReadOnlyImage loadImage(String id) throws ProvisionException {
        return new FSReadOnlyImage(this, id);
    }

    public FSReadOnlyImage loadLatest() throws ProvisionException {
        final String sessionId = getLastSessionId();
        if(sessionId == null) {
            return null;
        }
        return loadImage(sessionId);
    }
}
