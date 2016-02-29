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

package forg.jboss.provision.fs.test.add;

import org.jboss.provision.test.util.FSAssert;
import org.jboss.provision.test.util.TreeUtil;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class AddContentTestCase extends FSTestBase {

    @Test
    public void testMain() throws Exception {

        env.newImage()
            .write("a", "a/aa/aaa.txt", "userA")
            .write("a", "aaa.txt", "userA")
            .write("b", "a/aa/bbb.txt", "userB")
            .write("c", "c/cc/ccc.txt", "userC")
            .commit();

        TreeUtil.logTree(homeDir);

        env.newImage()
            .write("aa", "a/aa/aaa.txt", "userA")
            .write("aa", "a/aa.txt", "userA")
            .delete("a/aa/bbb.txt", "userB")
            .delete("c", "userC")
            .write("d", "d.txt", "userD")
            .commit();

        TreeUtil.logTree(homeDir);

        FSAssert.assertPaths(env,
                "aaa.txt",
                "a/aa/aaa.txt",
                "a/aa.txt",
                "d.txt");
    }
}
