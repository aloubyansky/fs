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
package org.jboss.provision.fs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.util.IoUtils;
import org.jboss.provision.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class FileUtils {

    static final String LS = PropertyUtils.getSystemProperty("line.separator");

    public static String readFile(File f) throws IOException {
        assert f != null : ProvisionErrors.nullArgument("file");
        BufferedReader reader = null;
        final StringWriter writer = new StringWriter();
        try {
            reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();
            if(line != null) {
                writer.write(line);
                line = reader.readLine();
                while(line != null) {
                    writer.write(LS);
                    writer.write(line);
                    line = reader.readLine();
                }
            }
        } finally {
            IoUtils.safeClose(reader);
            IoUtils.safeClose(writer);
        }
        return writer.getBuffer().toString();
    }

    public static List<String> readList(File f) throws IOException {
        assert f != null : ProvisionErrors.nullArgument("file");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();
            if(line == null) {
                return Collections.emptyList();
            }
            List<String> list = Collections.singletonList(line);
            line = reader.readLine();
            if(line == null) {
                return list;
            }
            list = new ArrayList<String>(list);
            while(line != null) {
                list.add(line);
                line = reader.readLine();
            }
            return list;
        } finally {
            IoUtils.safeClose(reader);
        }
    }

    public static Properties loadProperties(File f) throws IOException {
        final Properties props = new Properties();
        FileReader reader = null;
        try {
            reader = new FileReader(f);
            props.load(reader);
        } finally {
            IoUtils.safeClose(reader);
        }
        return props;
    }

    public static void writeProperties(File f, final Properties props) throws IOException {
        if(!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
            throw new IOException(ProvisionErrors.couldNotCreateDir(f.getParentFile()));
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(f);
            props.store(writer, null);
        } finally {
            IoUtils.safeClose(writer);
        }
    }
}
