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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
class PathsOwnership {

    private static final String OWNERSHIPS_DIR_NAME = "ownership";
    private static final String EXTERNAL_USER_TRUE = "externalUser=true";

    static class PathOwnership {
        boolean externalUser;
        Set<String> users;

        PathOwnership(boolean externalUser) {
            this.externalUser = externalUser;
            users = Collections.emptySet();
        }
        PathOwnership(String user) {
            users = Collections.singleton(user);
        }
        boolean isExternalUser() {
            return externalUser;
        }
        Set<String> getUsers() {
            return users;
        }
        boolean addUser(String user) {
            switch(users.size()) {
                case 0:
                    users = Collections.singleton(user);
                    return true;
                case 1:
                    if(users.contains(user)) {
                        return false;
                    }
                    users = new HashSet<String>(users);
                default:
                    return users.add(user);
            }
        }
        /**
         * Returns true if author was actually removed for the path
         */
        boolean removeUser(String user) {
            switch(users.size()) {
                case 0:
                    return false;
                case 1:
                    if(!users.contains(user)) {
                        return false;
                    }
                    users = Collections.emptySet();
                    return true;
                default:
                    return users.remove(user);
            }
        }
        boolean isOwned() {
            return !users.isEmpty() || externalUser;
        }
        public boolean isOwnedBy(String user) {
            return users.contains(user);
        }
    }

    static PathsOwnership getInstance(File historyDir) {
        return new PathsOwnership(new File(historyDir, OWNERSHIPS_DIR_NAME));
    }

    final File persistDir;
    private Map<String, PathOwnership> ownerships = Collections.emptyMap();
    private Map<String, Map<String, Boolean>> userPaths = Collections.emptyMap();

    private PathsOwnership(File persistDir) {
        this.persistDir = persistDir;
    }

    boolean isOwnedBy(String path, String user) throws ProvisionException {
        PathOwnership ownership = ownerships.get(path);
        if(ownership == null) {
            ownership = loadOwnership(path);
            if(ownership == null) {
                return false;
            }
            addOwnership(path, ownership);
        }
        return ownership.isOwnedBy(user);
    }

    void clear() {
        ownerships = Collections.emptyMap();
        userPaths = Collections.emptyMap();
    }

    void grab(String user, String path) throws ProvisionException {
        boolean added = false;
        PathOwnership ownership = ownerships.get(path);
        if(ownership != null) {
            added = ownership.addUser(user);
        } else {
            ownership = loadOwnership(path);
            if (ownership == null) {
                ownership = new PathOwnership(user);
                added = true;
            } else {
                added = ownership.addUser(user);
            }
            addOwnership(path, ownership);
        }

        if(added) {
            recordPath(user, path, true);
        }
    }

    boolean giveUp(String user, String path) throws ProvisionException {
        return giveUp(user, path, false);
    }

    boolean giveUp(String user, String path, boolean safe) throws ProvisionException {
        boolean removed = false;
        PathOwnership ownership = ownerships.get(path);
        if (ownership != null) {
            removed = ownership.removeUser(user);
        } else {
            ownership = loadOwnership(path);
            if (ownership != null) {
                addOwnership(path, ownership);
                removed = ownership.removeUser(user);
            }
        }
        if(removed) {
            recordPath(user, path, false);
            return ownership.isOwned();
        }
        if(safe) {
            return false;
        }
        throw ProvisionErrors.userDoesNotOwnTargetPath(user, path);
    }

    void schedulePersistence(MutableEnvImage fsImage) throws ProvisionException {
        for(Map.Entry<String, PathOwnership> entry : ownerships.entrySet()) {
            final PathOwnership ownership = entry.getValue();
            if(ownership.isOwned()) {
                final StringBuilder writer = new StringBuilder();
                if(ownership.isExternalUser()) {
                    writer.append(EXTERNAL_USER_TRUE).append(FileUtils.LS);
                }
                for(String user : ownership.getUsers()) {
                    writer.append(user).append(FileUtils.LS);
                }
                fsImage.write(writer.toString(), new File(persistDir, FSEnvironment.getFSRelativePath(entry.getKey())));
            } else {
                fsImage.delete(new File(persistDir, FSEnvironment.getFSRelativePath(entry.getKey())));
            }
        }
    }

    private void recordPath(String user, String path, boolean added) {
        Map<String, Boolean> paths = userPaths.get(user);
        if(paths == null) {
            switch(userPaths.size()) {
                case 0:
                    userPaths = Collections.singletonMap(user, Collections.singletonMap(path, added));
                    break;
                case 1:
                    userPaths = new HashMap<String, Map<String, Boolean>>(userPaths);
                default:
                    userPaths.put(user, Collections.singletonMap(path, added));
            }
        } else {
            if(paths.size() == 1) {
                if(paths.containsKey(path)) {
                    paths = Collections.singletonMap(path, added);
                } else {
                    paths = new HashMap<String, Boolean>(paths);
                    paths.put(path, true);
                }
                if(userPaths.size() == 1) {
                    userPaths = Collections.singletonMap(user, paths);
                } else {
                    userPaths.put(user, paths);
                }
            } else {
                paths.put(path, added);
            }
        }
    }

    private void addOwnership(String path, PathOwnership ownership) {
        switch (ownerships.size()) {
            case 0:
                ownerships = Collections.singletonMap(path, ownership);
                break;
            case 1:
                ownerships = new HashMap<String, PathOwnership>(ownerships);
            default:
                ownerships.put(path, ownership);
        }
    }

    private PathOwnership loadOwnership(String relativePath) throws ProvisionException {
        final String[] steps = relativePath.split("/");
        final File ownershipFile = IoUtils.newFile(persistDir, steps);
        if(!ownershipFile.exists()) {
            return null;
        }
        if(ownershipFile.isDirectory()) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(ownershipFile));
            String line = reader.readLine();
            if(line == null) {
                return null;
            }
            final PathOwnership ownership;
            if(EXTERNAL_USER_TRUE.equals(line)) {
                ownership = new PathOwnership(true);
            } else {
                ownership = new PathOwnership(line);
            }
            line = reader.readLine();
            while(line != null) {
                ownership.addUser(line);
                line = reader.readLine();
            }
            return ownership;
        } catch(IOException e) {
            throw ProvisionErrors.readError(ownershipFile, e);
        } finally {
            IoUtils.safeClose(reader);
        }
    }
}
