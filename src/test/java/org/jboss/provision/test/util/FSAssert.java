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

package org.jboss.provision.test.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provision.fs.EnvImage;
import org.jboss.provision.fs.FSEnvironment;
import org.jboss.provision.fs.FileUtils;
import org.jboss.provision.fs.UserImage;
import org.junit.Assert;

/**
 *
 * @author Alexey Loubyansky
 */
public class FSAssert {

    private FSAssert() {
    }

    public static void assertPaths(String user, FSEnvironment env, String... paths) throws Exception {
        final EnvImage envImage = env.getImage();
        if(envImage == null) {
            Assert.fail("Environment is empty");
        }
        final UserImage userImage = envImage.getUserImage(user);
        if(userImage == null) {
            Assert.fail("User " + user + " is not known.");
        }
        assertPaths(userImage, paths);
    }

    public static void assertPaths(UserImage image, String... paths) throws Exception {
        final Set<String> actual = image.getPaths();
        if(actual.size() != paths.length || !actual.containsAll(Arrays.asList(paths))) {
            Assert.fail("Expected " + Arrays.asList(paths) + ", actual " + actual);
        }
    }

    public static void assertUsers(FSEnvironment env, String... users) throws Exception {
        final EnvImage image = env.getImage();
        if(image == null) {
            if(users.length > 0) {
                Assert.fail("Environment is empty");
            }
            return;
        }
        assertUsers(image, users);
    }

    public static void assertUsers(EnvImage image, String... users) throws Exception {
        final List<String> actual = image.getUsers();
        if(actual.size() != users.length || !actual.containsAll(Arrays.asList(users))) {
            Assert.fail("Expected " + Arrays.asList(users) + ", actual " + actual);
        }
    }

    public static void assertNoContent(FSEnvironment env) throws Exception {
        assertPaths(env);
    }

    public static void assertContent(File f, String content) throws Exception {
        Assert.assertEquals(content, FileUtils.readFile(f));
    }

    public static void assertPaths(FSEnvironment env, String... path) throws Exception {
        final File envHome = env.getHomeDir();
        final Leaf root = new Leaf(null, envHome.getName());
        for(String p : path) {
            root.add(p);
        }
        assertChildren(root, envHome, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !".fs".equals(name);
            }});
    }

    private static void assertChildren(Leaf l, File f, FilenameFilter filter) {
        final File[] fList = filter == null ? f.listFiles() : f.listFiles(filter);
        if(fList == null) {
            if(!l.children.isEmpty()) {
                Assert.fail(l.getPath() + " is expected to be a directory containing " + l.children.keySet());
            }
        } else {
            if(fList.length == 0) {
                if(!l.children.isEmpty()) {
                    Assert.fail(l.getPath() + " is expected to contain " + l.children.keySet());
                }
            } else {
                if(l.children.isEmpty()) {
                    final StringBuilder buf = new StringBuilder(l.getPath());
                    buf.append(" is expected to be empty but contains ");
                    int i = fList.length;
                    for(File file : fList) {
                        buf.append(file.getName());
                        if(--i > 0) {
                            buf.append(", ");
                        }
                    }
                    Assert.fail(buf.toString());
                }
                final Map<String, Leaf> expected = new HashMap<String, Leaf>(l.children);
                for(File actual : fList) {
                    final Leaf leaf = expected.remove(actual.getName());
                    if(leaf == null) {
                        Assert.fail(l.getPath() + " is not expected to contain " + actual.getName());
                    }
                    assertChildren(leaf, actual, null);
                }
                if(!expected.isEmpty()) {
                    Assert.fail(l.getPath() + " is missing " + expected.keySet());
                }
            }
        }
    }

    private static class Leaf {
        final Leaf parent;
        final String name;
        Map<String, Leaf> children = Collections.emptyMap();
        Leaf(Leaf parent, String name) {
            this.parent = parent;
            this.name = name;
        }
        String getPath() {
            final StringBuilder buf = new StringBuilder();
            getPath(buf, this);
            return buf.toString();

        }
        private static void getPath(StringBuilder buf, Leaf l) {
            if(l.parent != null) {
                getPath(buf, l.parent);
                buf.append('/');
            }
            buf.append(l.name);
        }
        private Leaf newLeaf(String name) {
            final Leaf leaf = new Leaf(this, name);
            switch(children.size()) {
                case 0:
                    children = Collections.singletonMap(name, leaf);
                    break;
                case 1:
                    children = new HashMap<String, Leaf>(children);
                default:
                    children.put(name, leaf);
            }
            return leaf;
        }
        void add(String path) {
            final String[] split = path.split("/");
            Leaf leaf = this;
            for(String name : split) {
                Leaf child = leaf.children.get(name);
                if(child == null) {
                    child = leaf.newLeaf(name);
                }
                leaf = child;
            }
        }
        @SuppressWarnings("unused")
        void logTree() {
            buildTree(this, System.out, new LinkedList<Boolean>());
        }
        private static void buildTree(Leaf f, PrintStream out, LinkedList<Boolean> depth) {
            if(!depth.isEmpty()) {
                for(int i = 0; i < depth.size() - 1; ++i) {
                    if(depth.get(i)) {
                        out.print("|  ");
                    } else {
                        out.print("   ");
                    }
                }
                if(depth.getLast()) {
                    out.print("|--");
                } else {
                    out.print("`--");
                }
            }
            out.println(f.name);
            if(!f.children.isEmpty()) {
                final Iterator<Leaf> i = f.children.values().iterator();
                while(i.hasNext()) {
                    final Leaf c = i.next();
                    depth.addLast(i.hasNext());
                    buildTree(c, out, depth);
                    depth.removeLast();
                }
            }
        }
    }
}
