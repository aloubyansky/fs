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

import org.jboss.provision.ProvisionException;
import org.jboss.provision.test.util.FSAssert;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class DirectoriesTestCase extends FSTestBase {

    @Test
    public void testMain() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .mkdirs("a/b/c")
                .getEnvImage()
            .getUserImage("userB")
                .mkdirs("a/b/c/d")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "a/b/c/d");

        env.newImage()
            .getUserImage("userA")
                .delete("a/b/c")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "a/b");

        env.newImage()
            .getUserImage("userA")
                .delete("a/b")
                .getEnvImage()
            .getUserImage("userB")
                .mkdirs("a/b/c")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "a/b/c");

        env.newImage()
            .getUserImage("userB")
                .delete("a/b/c")
                .getEnvImage()
            .getUserImage("userA")
                .mkdirs("a/b/c/d")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "a/b/c/d");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "a/b/c");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "a/b");

        env.undoLastCommit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env);
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths(env, "a/b/c/d");

        env.undoLastCommit();

        FSAssert.assertNoContent(env);
    }

    @Test
    public void testUndoFileWrite() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("test", "a/b/c/d.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA");
        FSAssert.assertPaths("userA", env, "a/b/c/d.txt");
        FSAssert.assertPaths(env, "a/b/c/d.txt");

        env.undoLastCommit();
    }

    @Test
    public void testGiveUpDir() throws Exception {

        env.newImage()
            .getUserImage("userA")
                .write("a", "common/a/a.txt")
                .getEnvImage()
            .getUserImage("userB")
                .write("b", "common/b/b.txt")
                .getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "common/a/a.txt");
        FSAssert.assertPaths("userB", env, "common/b/b.txt");
        FSAssert.assertPaths(env, "common/a/a.txt", "common/b/b.txt");

        try {
            env.newImage().getUserImage("userA").delete("common").getEnvImage().commit();
            Assert.fail("can't remove not own content");
        } catch(ProvisionException e) {
        }

        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "common/a/a.txt");
        FSAssert.assertPaths("userB", env, "common/b/b.txt");
        FSAssert.assertPaths(env, "common/a/a.txt", "common/b/b.txt");

        env.undoLastCommit();

        FSAssert.assertNoContent(env);
    }
}
