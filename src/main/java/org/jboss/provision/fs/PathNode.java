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

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author Alexey Loubyansky
 */
class PathNode {

    private static void getPath(StringBuilder buf, PathNode l) {
        if(l.parent.parent != null) {
            getPath(buf, l.parent);
            buf.append('/');
        }
        buf.append(l.f.getName());
    }

    final PathNode parent;
    final File f;
    protected ContentTask contentTask;
    protected PathOwnership ownership;
    protected Map<String, PathNode> children = Collections.emptyMap();

    PathNode(PathNode parent, File f) {
        this.parent = parent;
        this.f = f;
    }

    ContentTask getContentTask() {
        return contentTask;
    }

    boolean isDeleted() {
        return contentTask != null && contentTask.isDelete();
    }

    int getChildrenTotal() {
        if(!f.isDirectory()) {
            return 0;
        }
        final String[] actual = f.list();
        if(actual.length == 0) {
            int count = 0;
            for(PathNode child : children.values()) {
                if(child.contentTask != null && !child.contentTask.isDelete()) {
                    ++count;
                }
            }
            return count;
        }
        final Set<String> expected = new HashSet<String>(Arrays.asList(actual));
        for(PathNode child : children.values()) {
            if(child.getContentTask() != null) {
                if(child.getContentTask().isDelete()) {
                    expected.remove(child.f.getName());
                } else {
                    expected.add(child.f.getName());
                }
            }
        }
        return expected.size();
    }

    String getPath() {
        final StringBuilder buf = new StringBuilder();
        getPath(buf, this);
        return buf.toString();

    }

    void logTree() {
        buildTree(this, System.out, new LinkedList<Boolean>());
    }
    private static void buildTree(PathNode node, PrintStream out, LinkedList<Boolean> depth) {
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
        out.print(node.f.getName());
        if(node.contentTask != null) {
            out.print(" [");
            out.print(node.contentTask.isDelete() ? "deleted" : "written");
            out.println(']');
        } else {
            out.println();
        }
        if(!node.children.isEmpty()) {
            final Iterator<PathNode> i = node.children.values().iterator();
            while(i.hasNext()) {
                final PathNode c = i.next();
                depth.addLast(i.hasNext());
                buildTree(c, out, depth);
                depth.removeLast();
            }
        }
    }
}
