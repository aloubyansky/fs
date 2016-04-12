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
import org.jboss.provision.test.util.TreeUtil;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeleteUserTestCase extends FSTestBase {

    @Test
    public void testSingleUserCommits() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "a/aa/aaa.txt")
                .write("a", "aaa.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertPaths(env,
                "aaa.txt",
                "a/aa/aaa.txt");
        FSAssert.assertUsers(env, "userA");
        FSAssert.assertPaths("userA", env, "aaa.txt", "a/aa/aaa.txt");

        env.newImage()
            .getUserImage("userA")
                .write("a2", "a/aa/aaa.txt")
                .write("a", "a2.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertPaths(env,
                "aaa.txt",
                "a2.txt",
                "a/aa/aaa.txt");
        FSAssert.assertUsers(env, "userA");
        FSAssert.assertPaths("userA", env, "aaa.txt", "a2.txt", "a/aa/aaa.txt");

        env.deleteUser("userA");
    }

    @Test
    public void testSingleUserCommitsMixedContent() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "a/aa/aaa.txt")
                .write("a", "shared.txt")
                .getEnvImage()
            .commit();

        env.newImage()
            .getUserImage("userB")
                .write("b", "a/aa/bbb.txt")
                .write("b", "shared.txt")
                .getEnvImage()
            .commit();

        env.newImage()
            .getUserImage("userA")
                .write("a2", "a.txt")
                .write("a2", "shared.txt")
                .getEnvImage()
            .commit();

        env.newImage()
            .getUserImage("userB")
                .write("b", "b/bb.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertPaths(env,
                "a.txt",
                "shared.txt",
                "b/bb.txt",
                "a/aa/aaa.txt",
                "a/aa/bbb.txt");
        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "shared.txt", "a/aa/aaa.txt", "a.txt");
        FSAssert.assertPaths("userB", env, "shared.txt", "a/aa/bbb.txt", "b/bb.txt");

        env.deleteUser("userA");

        FSAssert.assertPaths(env,
                "shared.txt",
                "b/bb.txt",
                "a/aa/bbb.txt");
        FSAssert.assertUsers(env, "userB");
        FSAssert.assertPaths("userB", env, "shared.txt", "a/aa/bbb.txt", "b/bb.txt");

        env.undoLastCommit();

        FSAssert.assertPaths(env,
                "shared.txt",
                "a/aa/bbb.txt");
        FSAssert.assertUsers(env, "userB");
        FSAssert.assertPaths("userB", env, "shared.txt", "a/aa/bbb.txt");

        System.out.println("undo");
        env.undoLastCommit();
        TreeUtil.logTree(env.getHomeDir());

        FSAssert.assertNoContent(env);
    }
}
