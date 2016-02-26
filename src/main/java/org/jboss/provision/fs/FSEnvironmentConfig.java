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
import org.jboss.provision.ProvisionErrors;

/**
 *
 * @author Alexey Loubyansky
 */
public class FSEnvironmentConfig {

    static final String DEFAULT_BACKUP_SUFFIX = ".fsbkp";
    static final String DEFAULT_HISTORY_DIR_NAME = ".fs";

    public static class Builder {

        File homeDir;
        File historyDir;

        private Builder() {
        }

        public Builder setHomeDir(File homeDir) {
            this.homeDir = homeDir;
            return this;
        }

        public Builder setHistoryDir(File historyDir) {
            this.historyDir = historyDir;
            return this;
        }

        public FSEnvironmentConfig build() {
            return new FSEnvironmentConfig(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    File homeDir;
    File historyDir;

    private FSEnvironmentConfig(Builder builder) {
        assert builder.homeDir != null : ProvisionErrors.nullArgument("homeDir");
        this.homeDir = builder.homeDir;
        if(builder.historyDir == null) {
            historyDir = new File(homeDir, DEFAULT_HISTORY_DIR_NAME);
        } else {
            historyDir = builder.historyDir;
        }
    }
}
