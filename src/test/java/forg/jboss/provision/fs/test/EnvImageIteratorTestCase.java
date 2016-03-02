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

package forg.jboss.provision.fs.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jboss.provision.fs.EnvImage;
import org.jboss.provision.test.util.FSAssert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class EnvImageIteratorTestCase extends FSTestBase {

    @Test
    public void testEmptyEnv() throws Exception {
        final Iterator<EnvImage> envHistory = env.envHistory();
        assertFalse(envHistory.hasNext());
        try {
            envHistory.next();
            fail("cannot do next");
        } catch(NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void testMain() throws Exception {

        env.newImage().getUserImage("userA").write("a", "a.txt").getEnvImage().commit();
        Iterator<EnvImage> envHistory = env.envHistory();
        assertTrue(envHistory.hasNext());
        EnvImage next = envHistory.next();
        FSAssert.assertUsers(next, "userA");
        FSAssert.assertPaths(next.getUserImage("userA"), "a.txt");
        assertFalse(envHistory.hasNext());

        env.newImage().getUserImage("userB").write("b", "b/b.txt").getEnvImage().commit();
        envHistory = env.envHistory();
        assertTrue(envHistory.hasNext());
        next = envHistory.next();
        FSAssert.assertUsers(next, "userA", "userB");
        FSAssert.assertPaths(next.getUserImage("userA"), "a.txt");
        FSAssert.assertPaths(next.getUserImage("userB"), "b/b.txt");
        assertTrue(envHistory.hasNext());
        next = envHistory.next();
        FSAssert.assertUsers(next, "userA");
        FSAssert.assertPaths(next.getUserImage("userA"), "a.txt");
        assertFalse(envHistory.hasNext());

        env.newImage()
            .getUserImage("userA").write("aa", "b/a.txt").getEnvImage()
            .getUserImage("userB").delete("b/b.txt").getEnvImage()
            .getUserImage("userC").write("c", "c.txt").getEnvImage()
            .commit();
        envHistory = env.envHistory();
        assertTrue(envHistory.hasNext());
        next = envHistory.next();
        FSAssert.assertUsers(next, "userA", "userB", "userC");
        FSAssert.assertPaths(next.getUserImage("userA"), "a.txt", "b/a.txt");
        FSAssert.assertPaths(next.getUserImage("userB"));
        FSAssert.assertPaths(next.getUserImage("userC"), "c.txt");
        assertTrue(envHistory.hasNext());
        next = envHistory.next();
        FSAssert.assertUsers(next, "userA", "userB");
        FSAssert.assertPaths(next.getUserImage("userA"), "a.txt");
        FSAssert.assertPaths(next.getUserImage("userB"), "b/b.txt");
        assertTrue(envHistory.hasNext());
        next = envHistory.next();
        FSAssert.assertUsers(next, "userA");
        FSAssert.assertPaths(next.getUserImage("userA"), "a.txt");
        assertFalse(envHistory.hasNext());
    }
}
