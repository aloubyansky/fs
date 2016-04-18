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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
class PathOwnership {
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
    boolean isOnlyOwner(String user) {
        return users.size() == 1 && users.contains(user);
    }
}