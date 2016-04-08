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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class UserHistory extends FSSessionHistory {

    private static final String USERS_DIR_NAME = "users";
    private static final String BACKUP = "backup";


    private static File getUsersDir(FSEnvironment env) {
        return new File(env.getHistoryDir(), USERS_DIR_NAME);
    }

    private static File getUserHistoryDir(FSEnvironment env, String user) {
        return new File(getUsersDir(env), user);
    }

    static File getUserImageDir(FSEnvironment env, String user, String imageId) {
        return new File(getUserHistoryDir(env, user), imageId);
    }

    static List<String> listUsers(FSEnvironment env) {
        final File usersDir = getUsersDir(env);
        if(!usersDir.exists()) {
            return Collections.emptyList();
        }
        return Arrays.asList(usersDir.list());
    }

    static File getBackupPath(UserImage user, String relativePath) {
        return IoUtils.newFile(user.getSessionDir(), BACKUP, FSEnvironment.getFSRelativePath(relativePath));
    }

    static File getBackupPath(String user, EnvImage fsImage, String relativePath) {
        return IoUtils.newFile(getUserImageDir(fsImage.getFSEnvironment(), user, fsImage.sessionId), BACKUP, FSEnvironment.getFSRelativePath(relativePath));
    }

    static UserImage loadUserImage(FSEnvironment env, String user, String imageId) throws ProvisionException {
        File dir = getUserHistoryDir(env, user);
        if(!dir.exists()) {
            throw ProvisionErrors.unknownUnit(user);
        }
        dir = getLastUpdateDir(dir, imageId);
        if(dir == null) {
            return null;
        }
        return new UserHistory(env, user).loadImage(dir.getName());
    }

    static List<String> listUsers(FSEnvironment env, String sessionId) throws ProvisionException {
        final List<String> allUsers = listUsers(env);
        if(allUsers.isEmpty()) {
            return Collections.emptyList();
        }
        if(allUsers.size() == 1) {
            final File imagePath = getLastUpdateDir(getUserHistoryDir(env, allUsers.get(0)), sessionId);
            if(imagePath == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(allUsers.get(0));
        }
        final List<String> users = new ArrayList<String>(allUsers.size());
        for(String user : allUsers) {
            final File imagePath = getLastUpdateDir(getUserHistoryDir(env, user), sessionId);
            if(imagePath != null) {
                users.add(user);
            }
        }
        return users;
    }

    private static File getLastUpdateDir(File userHistoryDir, String sessionId) throws ProvisionException {
        File sessionPath = new File(userHistoryDir, sessionId);
        if(!sessionPath.exists()) {
            return null;
        }
        if(sessionPath.isFile()) {
            try {
                sessionId = FileUtils.readFile(sessionPath);
            } catch (IOException e) {
                throw ProvisionErrors.readError(sessionPath, e);
            }
            sessionPath = new File(userHistoryDir, sessionId);
            if(!sessionPath.exists()) {
                throw ProvisionErrors.pathDoesNotExist(sessionPath);
            }
        }
        return sessionPath;
    }

    static void undo(MutableEnvImage envImage, String imageId) throws ProvisionException {
        final List<String> allUsers = listUsers(envImage.getFSEnvironment());
        if(allUsers.isEmpty()) {
            return;
        }
        for(String user : allUsers) {
            final File imagePath = getUserImageDir(envImage.getFSEnvironment(), user, imageId);
            if(imagePath.isDirectory()) {
                loadUserImage(envImage.getFSEnvironment(), user, imageId).undo(envImage);
            } else {
                envImage.delete(imagePath);
            }
        }
    }

    static void deleteUser(MutableEnvImage envImage, String user) throws ProvisionException {
        final FSEnvironment env = envImage.getFSEnvironment();
        final UserHistory userHistory = new UserHistory(env, user);
        if(!userHistory.getHistoryDir().exists()) {
            throw ProvisionErrors.unknownUnit(user);
        }
        UserImage userImage = userHistory.loadLatest();
        if(userImage == null) {
            throw ProvisionErrors.noHistoryRecordedUntilThisPoint();
        }

        for(String path : userImage.getPaths()) {
            envImage.delete(path, user, false);
        }

        env.getImage(userImage.sessionId).scheduleDelete(envImage);
        String prevId = userImage.getPreviousRecordId();
        while(prevId != null) {
            userImage = userHistory.loadImage(prevId);
            final EnvImage prevEnvImage = env.getImage(prevId);
            prevId = userImage.getPreviousRecordId();
            prevEnvImage.scheduleDelete(envImage);
        }

        envImage.delete(userHistory.getHistoryDir());
    }


    private final String author;

    protected UserHistory(FSEnvironment env, String author) {
        super(getUserHistoryDir(env, author));
        this.author = author;
    }

    List<String> getImageIds() {
        return Arrays.asList(historyDir.list(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }}));
    }

    MutableUserImage newImage(MutableEnvImage fsImage) {
        return new MutableUserImage(this, author, fsImage);
    }

    UserImage loadImage(String imageId) throws ProvisionException {
        return new UserImage(this, author, imageId);
    }

    UserImage loadLatest() throws ProvisionException {
        final String imageId = getLastSessionId();
        if(imageId == null) {
            return null;
        }
        return new UserImage(this, author, imageId);
    }
}
