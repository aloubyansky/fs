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

package org.jboss.provision.fs.test;

import org.jboss.provision.test.util.FSAssert;
import org.jboss.provision.test.util.FSUtils;
import org.jboss.provision.test.util.TreeUtil;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConcurrentUpdatesTestCase extends FSTestBase {

    @Test
    public void testConcurrentAdd() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.newImage()
            .getUserImage("userA")
                .delete("test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.undoLastCommit();

        FSAssert.assertNoContent(env);
    }

    @Test
    public void testLastAddWins() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.newImage()
            .getUserImage("userB")
                .write("b", "test/test.txt")
                .getEnvImage()
            .getUserImage("userA")
                .write("a", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "a");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.undoLastCommit();

        FSAssert.assertNoContent(env);
    }

    @Test
    public void testAddRemove() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.newImage()
            .getUserImage("userA")
                .delete("test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b2", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b2");

        env.newImage()
            .getUserImage("userA")
                .write("a2", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .delete("test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "a2");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b2");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.undoLastCommit();
        FSAssert.assertNoContent(env);
    }

    @Test
    public void testOverrideExternalContent() throws Exception {

        FSUtils.writeFile(env.getFile("test/test.txt"), "test");

        env.newImage()
            .getUserImage("userA")
                .write("a", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.undoLastCommit();

        FSAssert.assertUsers(env);
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "test");
    }

    @Test
    public void testRemoveDirs() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.newImage()
            .getUserImage("userA")
                .delete("test")
                .getEnvImage()
            .getUserImage("userB")
                .delete("test")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertNoContent(env);

        TreeUtil.logTree(env.getHomeDir());

        env.undoLastCommit();

        TreeUtil.logTree(env.getHomeDir());

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.undoLastCommit();

        FSAssert.assertUsers(env);
        FSAssert.assertNoContent(env);
    }

    @Test
    public void testAddRemoveDirs() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.newImage()
            .getUserImage("userA")
                .delete("test")
                .getEnvImage()
            .getUserImage("userB")
                .write("b2", "test/test.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b2");

        env.newImage()
            .getUserImage("userA")
                .write("a2", "test/test.txt")
                .getEnvImage()
            .getUserImage("userB")
                .delete("test")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "a2");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b2");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "test/test.txt");
        FSAssert.assertPaths("userB", env, "test/test.txt");
        FSAssert.assertPaths(env, "test/test.txt");
        FSAssert.assertContent(env.getFile("test/test.txt"), "b");

        env.undoLastCommit();

        FSAssert.assertUsers(env);
        FSAssert.assertNoContent(env);
    }
}
