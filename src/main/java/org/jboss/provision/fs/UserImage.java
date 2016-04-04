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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class UserImage extends FSSession {

    static final String TASKS = "tasks.txt";
    private static final String PATHS = "paths.txt";

    protected final UserHistory history;
    protected final String username;
    private Set<String> paths;

    UserImage(UserHistory history, String username, String sessionId) {
        super(history, sessionId);
        this.history = history;
        this.username = username;
    }

    UserHistory getUserHistory() {
        return history;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getPaths() throws ProvisionException {
        if(paths != null) {
            return paths;
        }
        paths = new HashSet<String>();
        File pathsFile;
        if(sessionDir.exists()) {
            pathsFile = new File(sessionDir, PATHS);
        } else { // if the current dir does not exist then load the paths from the last committed image
            final File lastSessionDir = history.getLastSessionDir();
            if(lastSessionDir != null) {
                pathsFile = new File(lastSessionDir, PATHS);
            } else {
                return paths;
            }
        }
        if (pathsFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(pathsFile));
                String line = reader.readLine();
                while (line != null) {
                    paths.add(line);
                    line = reader.readLine();
                }
                return paths;
            } catch (FileNotFoundException e) {
                throw ProvisionErrors.pathDoesNotExist(pathsFile);
            } catch (IOException e) {
                throw ProvisionErrors.readError(pathsFile, e);
            } finally {
                IoUtils.safeClose(reader);
            }
        }
        return paths;
    }

    @Override
    protected void schedulePersistence(MutableEnvImage fsImage) throws ProvisionException {
        super.schedulePersistence(fsImage);
        fsImage.write(new ContentWriter(new File(sessionDir, PATHS)) {
            @Override
            public void write(BufferedWriter writer) throws IOException, ProvisionException {
                for(String path : getPaths()) {
                    writer.write(path);
                    writer.newLine();
                }
            }
        });
    }

    @SuppressWarnings("resource")
    @Override
    protected void scheduleDelete(MutableEnvImage fsImage) throws ProvisionException {

        final File tasksFile = new File(sessionDir, TASKS);
        if(!tasksFile.exists()) {
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(tasksFile));
            String line = reader.readLine();
            while (line != null) {
                final char action = line.charAt(0);
                final char contentType = line.charAt(1);
                final String relativePath = line.substring(2);
                if (relativePath == null) {
                    throw ProvisionErrors.unexpectedTaskFormat();
                }
                final File backupPath = UserHistory.getBackupPath(this, relativePath);
                if(backupPath.exists()) {
                    fsImage.write(backupPath, relativePath, username, false);
                } else if (action == 'c') {
                    fsImage.delete(relativePath, username, false);
                } else if(contentType == 'd') {
                    fsImage.mkdirs(relativePath, username);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IoUtils.safeClose(reader);
        }


        super.scheduleDelete(fsImage);
    }

}
