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
import java.io.IOException;
import java.util.UUID;

import org.jboss.provision.ProvisionErrors;
import org.jboss.provision.ProvisionException;
import org.jboss.provision.util.HashUtils;
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
class DeleteTask extends ContentTask {

    static ContentTask DELETE_FLAG = deleteFlag(new File(UUID.randomUUID().toString()));

    private static ContentTask deleteFlag(final File target) {
        return new ContentTask(target) {
            @Override
            public void execute() throws ProvisionException {
            }
            @Override
            public boolean isDelete() {
                return true;
            }
            @Override
            public void backup() throws ProvisionException {
            }
            @Override
            public void revert() throws ProvisionException {
            }
            @Override
            public void cleanup() throws ProvisionException {
            }
            @Override
            public String toString() {
                return "DeleteFlag for " + target.getAbsolutePath();
            }
        };
    };

    private final boolean ifEmpty;

    protected DeleteTask(File target) {
        this(target, false);
    }
    protected DeleteTask(File target, boolean ifEmpty) {
        super(target);
        this.ifEmpty = ifEmpty;
    }
    protected DeleteTask(File target, File backup, boolean cleanup) {
        super(target, backup, cleanup);
        ifEmpty = false;
    }
    @Override
    public boolean isDelete() {
        return true;
    }
    @Override
    public void execute() throws ProvisionException {
        if(ifEmpty) {
            if(target.list().length == 0) {
                IoUtils.recursiveDelete(target);
            }
        } else {
            IoUtils.recursiveDelete(target);
        }
    }
    @Override
    protected boolean canHashContent() {
        return target.exists() && target.isFile();
    }
    @Override
    protected byte[] getContentHash() throws ProvisionException {
        try {
            return HashUtils.hashFile(target);
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(target.getAbsolutePath(), e);
        }
    }
    @Override
    public String toString() {
        return "DeleteTask for " + target.getAbsolutePath();
    }
}