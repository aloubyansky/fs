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

import static org.junit.Assert.fail;

import org.jboss.provision.ProvisionException;
import org.jboss.provision.test.util.FSAssert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class UndoLastCommitTestCase extends FSTestBase {

    @Test
    public void testEmptyEnv() throws Exception {
        try {
            env.undoLastCommit();
            fail("cannot undo non-existing commits");
        } catch(ProvisionException e) {
            // expected
        }
    }

    @Test
    public void testMain() throws Exception {

        env.newImage().getUserImage("userA").write("a", "a.txt").getEnvImage().commit();
        env.newImage().getUserImage("userB").write("b", "b/b.txt").getEnvImage().commit();
        env.newImage()
            .getUserImage("userA").write("aa", "b/a.txt").getEnvImage()
            .getUserImage("userB").delete("b/b.txt").getEnvImage()
            .getUserImage("userC").write("c", "c.txt").getEnvImage()
            .commit();

        FSAssert.assertUsers(env, "userA", "userB", "userC");
        FSAssert.assertPaths("userA", env, "a.txt", "b/a.txt");
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths("userC", env, "c.txt");
        FSAssert.assertPaths(env, "a.txt", "b/a.txt", "c.txt");

        env.undoLastCommit();
        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "a.txt");
        FSAssert.assertPaths("userB", env, "b/b.txt");
        FSAssert.assertPaths(env, "a.txt", "b/b.txt");

        env.undoLastCommit();
        FSAssert.assertUsers(env, "userA");
        FSAssert.assertPaths("userA", env, "a.txt");
        FSAssert.assertPaths(env, "a.txt");

        env.undoLastCommit();
        FSAssert.assertUsers(env);
        FSAssert.assertNoContent(env);

        try {
            env.undoLastCommit();
            fail("cannot undo non-existing commits");
        } catch(ProvisionException e) {
            // expected
        }
    }

    @Test
    public void testMkDirs() throws Exception {

        env.newImage().getUserImage("userA").mkdirs("a/aa/aaa").getEnvImage().commit();
        FSAssert.assertUsers(env, "userA");
        FSAssert.assertPaths("userA", env);

        env.undoLastCommit();
        FSAssert.assertUsers(env);
        FSAssert.assertNoContent(env);
    }
}
