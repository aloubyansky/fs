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
import org.jboss.provision.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class ContentTask {


    protected final File target;
    protected File backup;
    private final boolean cleanup;

    ContentTask(File target) {
        //this(target, new File(target.getParentFile(), target.getName() + FSEnvironmentConfig.DEFAULT_BACKUP_SUFFIX), true);
        this(target, new File(IoUtils.getIoTmpDir(), UUID.randomUUID().toString()), true);
    }

    protected ContentTask(File target, File backup, boolean cleanup) {
        this.target = target;
        this.backup = backup;
        this.cleanup = cleanup;
    }

    public File getTarget() {
        return target;
    }

    public boolean isDelete() {
        return false;
    }

    public String getContentString() {
        throw new UnsupportedOperationException();
    }

    public File getContentFile() {
        throw new UnsupportedOperationException();
    }

    protected boolean canHashContent() {
        return false;
    }

    protected byte[] getContentHash() throws ProvisionException {
        throw new UnsupportedOperationException();
    }

    public void backup() throws ProvisionException {
        if (!target.exists()) {
            backup = null;
            return;
        }
        if (backup.exists()) {
            throw ProvisionErrors.pathAlreadyExists(backup);
        }
        try {
            IoUtils.copyFile(target, backup);
        } catch (IOException e) {
            throw ProvisionErrors.failedToCopyContent(e);
        }
    }

    public void revert() throws ProvisionException {
        if (backup == null) {
            return;
        }
        if (backup.isDirectory()) {
            IoUtils.recursiveDelete(target);
        }
        try {
            IoUtils.copyFile(backup, target);
        } catch (IOException e) {
            throw ProvisionErrors.failedToCopyContent(e);
        }
        IoUtils.recursiveDelete(backup);
        backup = null;
    }

    public void cleanup() throws ProvisionException {
        if(!cleanup) {
            return;
        }
        if (backup == null) {
            return;
        }
        IoUtils.recursiveDelete(backup);
        backup = null;
    }

    public abstract void execute() throws ProvisionException;

    @Override
    public String toString() {
        return "ContentTask for " + target.getAbsolutePath();
    }
}