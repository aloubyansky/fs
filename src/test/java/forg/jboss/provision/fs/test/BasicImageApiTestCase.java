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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.jboss.provision.fs.FSImage;
import org.jboss.provision.test.util.FSAssert;
import org.jboss.provision.test.util.FSUtils;
import org.jboss.provision.util.HashUtils;
import org.jboss.provision.util.IoUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class BasicImageApiTestCase extends FSTestBase {

    @Test
    public void testMain() throws Exception {

        env.newImage()
            .write("a", "a/aa/aaa.txt", "userA")
            .write("a", "aaa.txt", "userA")
            .write("b", "a/aa/bbb.txt", "userB")
            .write("c", "c/cc/ccc.txt", "userC")
            .commit();

        FSAssert.assertPaths(env,
                "aaa.txt",
                "a/aa/aaa.txt",
                "a/aa/bbb.txt",
                "c/cc/ccc.txt");

        env.newImage()
            .write("aa", "a/aa/aaa.txt", "userA")
            .write("aa", "a/aa.txt", "userA")
            .delete("a/aa/bbb.txt", "userB")
            .delete("c", "userC")
            .write("d", "d.txt", "userD")
            .mkdirs("f/g/h")
            .commit();

        FSAssert.assertPaths(env,
                "aaa.txt",
                "a/aa.txt",
                "a/aa/aaa.txt",
                "d.txt",
                "f/g/h");
    }

    @Test
    public void testContains() throws Exception {

        final FSImage fsImage = env.newImage();

        assertFalse(fsImage.contains("a.txt"));
        FSUtils.writeRandomContent(new File(env.getHomeDir(), "a.txt"));
        assertTrue(fsImage.contains("a.txt"));

        assertFalse(fsImage.contains("b.txt"));
        fsImage.write("b", "b.txt", "userB");
        assertFalse(new File(env.getHomeDir(), "b.txt").exists());
        assertTrue(fsImage.contains("b.txt"));

        assertFalse(fsImage.contains("a/b/c"));
        fsImage.mkdirs("a/b/c");
        assertFalse(IoUtils.newFile(env.getHomeDir(), "a", "b", "c").exists());
        assertTrue(fsImage.contains("a/b/c"));
    }

    @Test
    public void testReadContent() throws Exception {

        final FSImage fsImage = env.newImage();

        assertNull(fsImage.readContent("a/a.txt"));
        FSUtils.writeFile(IoUtils.newFile(env.getHomeDir(), "a", "a.txt"), "a text");
        assertEquals("a text", fsImage.readContent("a/a.txt"));

        assertNull(fsImage.readContent("a/aa.txt"));
        fsImage.write("aa text", "a/aa.txt", "userA");
        assertEquals("aa text", fsImage.readContent("a/aa.txt"));

        assertNull(fsImage.readContent("a/b/c"));
        fsImage.mkdirs("a/b/c");
        try {
            fsImage.readContent("a/b/c");
            fail("cannot read content of a dir");
        } catch(Exception e) {
            // expected
        }
    }

    @Test
    public void testIsDeleted() throws Exception {

        env.newImage()
            .write("abcd", "a/b/c/d.txt", "userA")
            .write("abc", "a/b/c.txt", "userA")
            .write("ab", "a/b.txt", "userA")
            .write("a", "a.txt", "userA")
            .commit();

        final FSImage fsImage = env.newImage();
        assertFalse(fsImage.isDeleted("a.txt"));
        fsImage.delete("a.txt", "userA");
        assertTrue(fsImage.isDeleted("a.txt"));
        assertFalse(fsImage.isDeleted("aa.txt"));

        assertFalse(fsImage.isDeleted("a/b/c.txt"));
        assertFalse(fsImage.isDeleted("a/b/c/d.txt"));
        fsImage.delete("a/b", "userA");
        assertTrue(fsImage.isDeleted("a/b/c.txt"));
        assertTrue(fsImage.isDeleted("a/b/c/d.txt"));
        assertTrue(fsImage.isDeleted("a/b/c"));
        assertTrue(fsImage.isDeleted("a/b"));

        assertFalse(fsImage.isDeleted("a/b.txt"));
    }

    @Test
    public void testGetHash() throws Exception {

        final FSImage fsImage = env.newImage();

        assertNull(fsImage.getHash("a/a.txt"));
        final File a = IoUtils.newFile(env.getHomeDir(), "a", "a.txt");
        FSUtils.writeFile(a, "a text");
        Assert.assertArrayEquals(HashUtils.hashFile(a), fsImage.getHash("a/a.txt"));

        final File aa = IoUtils.newFile(env.getHomeDir(), "aa", "aa.txt");
        FSUtils.writeFile(aa, "aa text");
        assertNull(fsImage.getHash("a/aa.txt"));
        fsImage.write(aa, "a/aa.txt", "userA");
        Assert.assertArrayEquals(HashUtils.hashFile(aa), fsImage.getHash("a/aa.txt"));
    }
}
