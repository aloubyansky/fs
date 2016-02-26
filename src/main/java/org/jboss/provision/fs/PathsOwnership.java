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
    private static final String EXTERNAL_AUTHOR_TRUE = "externalAuthor=true";

    static class PathOwnership {
        boolean externalAuthor;
        Set<String> authors;

        PathOwnership(boolean externalAuthor) {
            this.externalAuthor = externalAuthor;
        }
        PathOwnership(String author) {
            authors = Collections.singleton(author);
        }
        boolean isExternalAuthor() {
            return externalAuthor;
        }
        Set<String> getAuthors() {
            return authors;
        }
        boolean addAuthor(String author) {
            switch(authors.size()) {
                case 0:
                    authors = Collections.singleton(author);
                    return true;
                case 1:
                    if(authors.contains(author)) {
                        return false;
                    }
                    authors = new HashSet<String>(authors);
                default:
                    return authors.add(author);
            }
        }
        /**
         * Returns true if author was actually removed for the path
         */
        boolean removeAuthor(String author) {
            switch(authors.size()) {
                case 0:
                    return false;
                case 1:
                    if(!authors.contains(author)) {
                        return false;
                    }
                    authors = Collections.emptySet();
                    return true;
                default:
                    return authors.remove(author);
            }
        }
        boolean isOwned() {
            return !authors.isEmpty() || externalAuthor;
        }
    }

    static PathsOwnership getInstance(File historyDir) {
        return new PathsOwnership(new File(historyDir, OWNERSHIPS_DIR_NAME));
    }

    final File persistDir;
    private Map<String, PathOwnership> ownerships = Collections.emptyMap();
    private Map<String, Map<String, Boolean>> authorPaths = Collections.emptyMap();

    private PathsOwnership(File persistDir) {
        this.persistDir = persistDir;
    }

    void clear() {
        ownerships = Collections.emptyMap();
        authorPaths = Collections.emptyMap();
    }

    void addAuthor(String path, String author) throws ProvisionException {
        boolean added = false;
        PathOwnership ownership = ownerships.get(path);
        if(ownership != null) {
            added = ownership.addAuthor(author);
        } else {
            ownership = loadOwnership(path);
            if (ownership == null) {
                ownership = new PathOwnership(author);
                added = true;
            } else {
                added = ownership.addAuthor(author);
            }
            addOwnership(path, ownership);
        }

        if(added) {
            recordPath(author, path, true);
        }
    }

    boolean removeAuthor(String path, String author) throws ProvisionException {
        boolean removed = false;
        PathOwnership ownership = ownerships.get(path);
        if (ownership != null) {
            removed = ownership.removeAuthor(author);
        } else {
            ownership = loadOwnership(path);
            if (ownership != null) {
                addOwnership(path, ownership);
                removed = ownership.removeAuthor(author);
            }
        }
        if(removed) {
            recordPath(author, path, false);
        }
        return removed;
    }

    void schedulePersistence(FSImage fsImage) throws ProvisionException {
        for(Map.Entry<String, PathOwnership> entry : ownerships.entrySet()) {
            final PathOwnership ownership = entry.getValue();
            if(ownership.isOwned()) {
                final StringBuilder writer = new StringBuilder();
                if(ownership.isExternalAuthor()) {
                    writer.append(EXTERNAL_AUTHOR_TRUE).append(FileUtils.LS);
                }
                for(String author : ownership.getAuthors()) {
                    writer.append(author).append(FileUtils.LS);
                }
                fsImage.write(writer.toString(), new File(persistDir, FSEnvironment.getFSRelativePath(entry.getKey())));
            } else {
                fsImage.delete(new File(persistDir, FSEnvironment.getFSRelativePath(entry.getKey())));
            }
        }
    }

    private void recordPath(String author, String path, boolean added) {
        Map<String, Boolean> paths = authorPaths.get(author);
        if(paths == null) {
            switch(authorPaths.size()) {
                case 0:
                    authorPaths = Collections.singletonMap(author, Collections.singletonMap(path, added));
                    break;
                case 1:
                    authorPaths = new HashMap<String, Map<String, Boolean>>(authorPaths);
                default:
                    authorPaths.put(author, Collections.singletonMap(path, added));
            }
        } else {
            if(paths.size() == 1) {
                if(paths.containsKey(path)) {
                    paths = Collections.singletonMap(path, added);
                } else {
                    paths = new HashMap<String, Boolean>(paths);
                    paths.put(path, true);
                }
                if(authorPaths.size() == 1) {
                    authorPaths = Collections.singletonMap(author, paths);
                } else {
                    authorPaths.put(author, paths);
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
            if(EXTERNAL_AUTHOR_TRUE.equals(line)) {
                ownership = new PathOwnership(true);
            } else {
                ownership = new PathOwnership(line);
            }
            line = reader.readLine();
            while(line != null) {
                ownership.addAuthor(line);
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
