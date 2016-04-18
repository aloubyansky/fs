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
import org.jboss.provision.util.HashUtils;

/**
 * Paths
 * |- hash (hash of the path as dir name)
 * |  `-content (the actual content)
 * |- sessions.txt (ordered list of session ids in which the path was changed, last change at the top)
 * `- ownership (lists users changed the content of the path, excluding those the requested delete)
 *
 * @author Alexey Loubyansky
 */
class PathsRepository {

    private static final String HASH = "hash";
    private static final String OWNERSHIP = "ownership.txt";
    private static final String PATHS = "paths";
    private static final String SESSIONS = "sessions.txt";


    protected static class PathHistory {

        private final File pathDir;
        private File sessionsTxt;
        private File ownershipTxt;

        protected PathHistory(File pathDir) {
            this.pathDir = pathDir;
        }

        protected File getSessionsTxt() {
            if(sessionsTxt != null) {
                return sessionsTxt;
            }
            sessionsTxt = new File(pathDir, SESSIONS);
            return sessionsTxt;
        }

        protected File getOwnershipTxt() {
            if(ownershipTxt != null) {
                return ownershipTxt;
            }
            ownershipTxt = new File(pathDir, OWNERSHIP);
            return ownershipTxt;
        }

        protected boolean exists() {
            return pathDir.exists();
        }
    }

    private final File repoDir;

    protected PathsRepository(File envHistoryDir) {
        repoDir = new File(envHistoryDir, PATHS);
    }

    protected PathHistory getPathHistory(String path) throws ProvisionException {
        try {
            return new PathHistory(new File(repoDir, HashUtils.hashToHexString(path)));
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(path, e);
        }
    }
}
