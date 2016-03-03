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

import static org.junit.Assert.assertNull;

import org.jboss.provision.test.util.FSAssert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class GetUserImagesTestCase extends FSTestBase {

    @Test
    public void testMain() throws Exception {

        assertNull(env.getImage());

        env.newImage().getUserImage("userA").write("a", "a.txt").getEnvImage().commit();
        FSAssert.assertUsers(env, "userA");
        FSAssert.assertPaths("userA", env, "a.txt");

        env.newImage().getUserImage("userB").write("b", "b/b.txt").getEnvImage().commit();
        FSAssert.assertUsers(env, "userA", "userB");
        FSAssert.assertPaths("userA", env, "a.txt");
        FSAssert.assertPaths("userB", env, "b/b.txt");

        env.newImage()
            .getUserImage("userA").write("aa", "b/a.txt").getEnvImage()
            .getUserImage("userB").delete("b/b.txt").getEnvImage()
            .getUserImage("userC").write("c", "c.txt").getEnvImage()
            .commit();
        FSAssert.assertUsers(env, "userA", "userB", "userC");
        FSAssert.assertPaths("userA", env, "a.txt", "b/a.txt");
        FSAssert.assertPaths("userB", env);
        FSAssert.assertPaths("userC", env, "c.txt");
    }
}
