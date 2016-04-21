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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class UserHistory extends FSSessionHistory {

    private static final String USERS_DIR_NAME = "users";

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

    static void undo(MutableEnvImage envImage, String sessionId) throws ProvisionException {
        final List<String> allUsers = listUsers(envImage.getFSEnvironment());
        if(allUsers.isEmpty()) {
            return;
        }
        for(String user : allUsers) {
            final File imagePath = getUserImageDir(envImage.getFSEnvironment(), user, sessionId);
            if(imagePath.isDirectory()) {
                final File tasksFile = new File(imagePath, UserImage.TASKS);
                if(!tasksFile.exists()) {
                    continue;
                }

                final MutableUserImage userImage = envImage.getUserImage(user);
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(tasksFile));
                    String line = reader.readLine();
                    while (line != null) {
                        System.out.println("UserHistory.undo " + user + " " + line);
                        final char action = line.charAt(0);
                        final String relativePath = line.substring(1);
                        if (relativePath == null) {
                            throw ProvisionErrors.unexpectedTaskFormat();
                        }
                        if (action == UserImage.CREATE || action == UserImage.GRAB) {
                            envImage.giveUp(envImage.fsEnv.getFile(relativePath), relativePath, userImage, false);
                        } else if(action == UserImage.UPDATE) {
                        } else {
                            envImage.grab(relativePath, user);
                        }
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IoUtils.safeClose(reader);
                }

                loadUserImage(envImage.getFSEnvironment(), user, sessionId).scheduleDelete(envImage);
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
        UserImage prevUserImage = userHistory.loadLatest();
        if(prevUserImage == null) {
            throw ProvisionErrors.noHistoryRecordedUntilThisPoint();
        }

//        final MutableUserImage userImage = envImage.getUserImage(user);
//        for(String path : prevUserImage.getPaths()) {
//            envImage.delete(path, userImage, false);
//        }

        deleteCommitRecords(envImage, prevUserImage.sessionId, user);
        env.getImage(prevUserImage.sessionId).undo(envImage);//.scheduleDelete(envImage);
        String prevId = prevUserImage.getPreviousRecordId();
        while(prevId != null) {
            deleteCommitRecords(envImage, prevId, user);
            prevUserImage = userHistory.loadImage(prevId);
            final EnvImage prevEnvImage = env.getImage(prevId);
            prevId = prevUserImage.getPreviousRecordId();
            prevEnvImage.undo(envImage);//.scheduleDelete(envImage);
        }

        envImage.delete(userHistory.getHistoryDir());
    }

    static void deleteCommitRecords(MutableEnvImage envImage, String id, String user) throws ProvisionException {

        final File usersDir = getUsersDir(envImage.getFSEnvironment());
        if(!usersDir.exists()) {
            throw ProvisionErrors.noHistoryRecordedUntilThisPoint();
        }
        final String[] users = usersDir.list();
        if(users.length == 0) {
            throw ProvisionErrors.noHistoryRecordedUntilThisPoint();
        }
        Set<String> userSet = Collections.emptySet();
        for(String u : users) {
            final File record = IoUtils.newFile(usersDir, u, id);
            if(!record.exists()) {
                continue;
            }
            if(record.isDirectory()) {
                if(!userSet.isEmpty()) {
                    userSet.add(u);
                } else if(!u.equals(user)) {
                    userSet = new HashSet<String>();
                    userSet.add(user);
                    userSet.add(u);
                }
            } else {
                envImage.delete(record);
            }
        }
        if(!userSet.isEmpty()) {
            throw ProvisionErrors.instructionTargetsOtherThanRequestedUnits(user, new HashSet<String>(Arrays.asList(users)));
        }
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

    MutableUserImage loadMutableImage(MutableEnvImage fsImage, String sessionId) {
        return new MutableUserImage(this, author, fsImage, sessionId);
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
