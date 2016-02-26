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
class MkDirsWriter extends ContentWriter {
    private boolean created;
    protected MkDirsWriter(File target) {
        super(target);
    }
    @Override
    public void execute() throws ProvisionException {
        if(target.exists()) {
            return;
        }
        if(!target.mkdirs()) {
            throw new ProvisionException(ProvisionErrors.couldNotCreateDir(target));
        }
        created = true;
    }
    @Override
    public void write(BufferedWriter writer) throws IOException {
        throw new UnsupportedOperationException();
    }
    @Override
    public void backup() throws ProvisionException {
    }
    @Override
    public void revert() throws ProvisionException {
        if (created) {
            IoUtils.recursiveDelete(target);
        }
    }
    @Override
    public void cleanup() throws ProvisionException {
    }
}