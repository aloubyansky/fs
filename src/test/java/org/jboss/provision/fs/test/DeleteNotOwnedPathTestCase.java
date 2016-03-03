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

import static org.junit.Assert.fail;

import java.io.File;

import org.jboss.provision.ProvisionException;
import org.jboss.provision.test.util.FSAssert;
import org.jboss.provision.test.util.FSUtils;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class DeleteNotOwnedPathTestCase extends FSTestBase {

    @Test
    public void testMain() throws Exception {

        FSAssert.assertNoContent(env);
        assertEmptyDir(env.getHomeDir());
        FSUtils.writeFile(new File(env.getHomeDir(), "a.txt"), "original");
        assertNotEmptyDir(env.getHomeDir());

        try {
            env.newImage().getUserImage("userA").delete("a.txt");
            fail("cannot delete not own path");
        } catch(ProvisionException e) {
            // denied
        }

        env.newImage().getUserImage("userA").write("a", "a.txt").delete("a.txt").getEnvImage().commit();
        assertNotEmptyDir(env.getHomeDir());
        FSAssert.assertNoContent(env);
    }
}
