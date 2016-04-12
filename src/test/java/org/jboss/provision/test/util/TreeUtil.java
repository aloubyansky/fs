/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.jboss.provision.util.HashUtils;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class TreeUtil {

    public static interface Formatter {
        void append(PrintStream out, File f) throws IOException;
    }

    public static class DefaultFormatter implements Formatter {
        @Override
        public void append(PrintStream out, File f) throws IOException {
            if(f.isDirectory()) {
                appendDir(out, f);
            } else {
                appendFile(out, f);
            }
        }
        protected void appendDir(PrintStream out, File dir) throws IOException {
            out.print(dir.getName());
        }
        protected void appendFile(PrintStream out, File f) throws IOException {
            out.print(f.getName());
            out.print(" (hash=");
            out.print(HashUtils.bytesToHexString(HashUtils.hashFile(f)));
            out.print(')');
        }
    }

    private static class Formatters {
        private Map<String, Formatter> formatters = Collections.emptyMap();
        public void addFormatter(String path, Formatter formatter) {
            switch(formatters.size()) {
                case 0:
                    formatters = Collections.singletonMap('/' + path, formatter);
                    break;
                case 1:
                    formatters = new HashMap<String, Formatter>(formatters);
                default:
                    formatters.put('/' + path, formatter);
            }
        }
        Formatter getFormatter(String path, Formatter defFormatter) {
            return formatters.getOrDefault(path, defFormatter);
        }
    }

    private static final Formatter FORMATTER = new DefaultFormatter();
    private static final Formatters FORMATTERS = new Formatters();

    static {
        FORMATTERS.addFormatter(".fs/ownership", new DefaultFormatter() {
            @Override
            protected void appendFile(PrintStream out, File f) throws IOException {
                out.print(f.getName());
                out.print(" (");
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(f));
                    String line = reader.readLine();
                    if (line != null) {
                        out.print(line);
                        line = reader.readLine();
                        while (line != null) {
                            out.print(',');
                            out.print(line);
                            line = reader.readLine();
                        }
                    }
                } finally {
                    IoUtils.safeClose(reader);
                }
                out.print(')');
            }
        });
    }


    public static void logTree(File f) throws IOException {
        buildTree(f, System.out, new LinkedList<Boolean>(), "", FORMATTER);
    }

    private static void buildTree(File f, PrintStream out, LinkedList<Boolean> depth, String relativePath, Formatter defFormatter) throws IOException {

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
        final Formatter formatter = FORMATTERS.getFormatter(relativePath, defFormatter);
        formatter.append(out, f);
        if(f.isDirectory()) {
            out.println();
            final File[] files = f.listFiles();
            int i = 0;
            while(i < files.length) {
                final File c = files[i++];
                depth.addLast(i != files.length);
                buildTree(c, out, depth, relativePath + '/' + c.getName(), formatter);
                depth.removeLast();
            }
        } else {
            out.println();
        }
    }
}
