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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.util.IoUtils;
import org.jboss.provision.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class FSUtils {

    public static File createTmpDir(String name) {
        return IoUtils.createTmpDir(name);
    }

    public static File nextTmpDir(String prefix) {
        final File tmpDir = new File(PropertyUtils.getSystemProperty("java.io.tmpdir"));
        File f = new File(tmpDir, prefix);
        if(f.exists()) {
            int i = 1;
            f = new File(tmpDir, prefix + i++);
            while(f.exists()) {
                f = new File(tmpDir, prefix + i++);
            }
        }
        if(!f.mkdirs()) {
            throw new IllegalStateException("Failed to create " + f.getAbsolutePath());
        }
        return f;
    }

    public static File newTmpFile(String name) {
        return new File(IoUtils.getIoTmpDir(), name);
    }

    public static boolean isEmptyDirBranch(File dir) {
        if(!dir.isDirectory()) {
            return false;
        }
        for(File f : dir.listFiles()) {
            if(!isEmptyDirBranch(f)) {
                return false;
            }
        }
        return true;
    }

    public static void writeRandomContent(File f) throws IOException {
        writeFile(f, UUID.randomUUID().toString());
    }

    public static void writeFile(File f, String content) throws IOException {
        if(!f.getParentFile().exists()) {
            if(!f.getParentFile().mkdirs()) {
                throw new IOException(ProvisionErrors.couldNotCreateDir(f.getParentFile()));
            }
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(f);
            writer.write(content);
        } finally {
            IoUtils.safeClose(writer);
        }
    }
}
