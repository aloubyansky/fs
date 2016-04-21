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
import org.jboss.provision.util.HashUtils;

/**
 *
 * @author Alexey Loubyansky
 */
class StringContentWriter extends ContentWriter {
    private final String content;
    StringContentWriter(String content, File target) {
        super(target);
        assert content != null : ProvisionErrors.nullArgument("content");
        this.content = content;
    }
    StringContentWriter(String content, File target, File backup, boolean cleanup) {
        super(target, backup, cleanup);
        assert content != null : ProvisionErrors.nullArgument("content");
        this.content = content;
    }
    @Override
    public String getContentString() {
        return content;
    }
    @Override
    public void write(BufferedWriter writer) throws IOException {
        writer.write(content);
    }
    @Override
    protected boolean canHashContent() {
        return true;
    }
    @Override
    protected byte[] getContentHash() throws ProvisionException {
        try {
            return HashUtils.hashBytes(content.getBytes());
        } catch (IOException e) {
            throw ProvisionErrors.hashCalculationFailed(content, e);
        }
    }
    @Override
    public String toString() {
        return "StringContentWriter for " + target.getAbsolutePath();
    }
}