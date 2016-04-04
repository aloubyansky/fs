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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class CopyDirContentWriter extends ContentWriter {

    private final File src;

    CopyDirContentWriter(File src, File target) {
        super(target);
        this.src = src;
    }

    CopyDirContentWriter(File src, File target, File backup, boolean cleanup) {
        super(target, backup, cleanup);
        assert src != null : ProvisionErrors.nullArgument("src");
        this.src = src;
    }

    /* (non-Javadoc)
     * @see org.jboss.provision.fs.ContentTask#execute()
     */
    @Override
    public void execute() throws ProvisionException {
        try {
            IoUtils.copyFile(src, target);
        } catch (IOException e) {
            throw ProvisionErrors.writeError(target, e);
        }
    }

    @Override
    public String toString() {
        return "CopyDirContentWriter " + src.getAbsolutePath() + " -> " + target.getAbsolutePath();
    }

    @Override
    public void write(BufferedWriter writer) throws IOException, ProvisionException {
        throw new UnsupportedOperationException();
    }
}
