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

package org.jboss.provision.fs.test;

import java.io.File;
import java.io.IOException;

import org.jboss.provision.fs.FSEnvironment;
import org.jboss.provision.fs.FSEnvironmentConfig;
import org.jboss.provision.test.util.FSAssert;
import org.jboss.provision.test.util.FSUtils;
import org.jboss.provision.test.util.TreeUtil;
import org.jboss.provision.util.IoUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 *
 * @author Alexey Loubyansky
 */
public class FSTestBase {

    protected File homeDir;
    protected FSEnvironment env;

    @Before
    public void before() throws Exception {
        homeDir = FSUtils.createTmpDir("fstestsuite");
        env = FSEnvironment.create(FSEnvironmentConfig.builder().setHomeDir(homeDir).build());
        doBefore();
    }

    protected void doBefore() throws Exception {
    }

    @After
    public void after() throws Exception {
        IoUtils.recursiveDelete(homeDir);
        doAfter();
    }

    protected void doAfter() throws Exception {
    }

    protected void assertContent(String relativePath, String content) throws Exception {
        FSAssert.assertContent(env.getFile(relativePath), content);
    }

    protected void assertEmptyDir(File f) {
        String[] list = f.list();
        if(list == null) {
            throw new IllegalArgumentException(f.getAbsolutePath() + " is a file");
        }
        Assert.assertEquals(0, list.length);
    }

    protected void assertNotEmptyDir(File f) {
        String[] list = f.list();
        if(list == null) {
            throw new IllegalArgumentException(f.getAbsolutePath() + " is a file");
        }
        Assert.assertTrue(list.length > 0);
    }

    protected void logTree() {
        try {
            TreeUtil.logTree(env.getHomeDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
