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

package forg.jboss.provision.fs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.provision.ProvisionException;
import org.jboss.provision.fs.UserImage;
import org.jboss.provision.fs.FSEnvironment;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class GetUserImagesTestCase extends FSTestBase {

    @Test
    public void testMain() throws Exception {

        assertNull(env.loadLatest());

        env.newImage().getUserImage("userA").write("a", "a.txt").getEnvImage().commit();
        assertAuthors(env, "userA");
        assertAuthorPaths(env, "userA", "a.txt");

        env.newImage().getUserImage("userB").write("b", "b/b.txt").getEnvImage().commit();
        assertAuthors(env, "userA", "userB");
        assertAuthorPaths(env, "userA", "a.txt");
        assertAuthorPaths(env, "userB", "b/b.txt");

        env.newImage()
            .getUserImage("userA").write("aa", "b/a.txt").getEnvImage()
            .getUserImage("userB").delete("b/b.txt").getEnvImage()
            .getUserImage("userC").write("c", "c.txt").getEnvImage()
            .commit();
        assertAuthors(env, "userA", "userB", "userC");
        assertAuthorPaths(env, "userA", "a.txt", "b/a.txt");
        assertAuthorPaths(env, "userB");
        assertAuthorPaths(env, "userC", "c.txt");
    }

    private static void assertAuthorPaths(FSEnvironment env, String author, String... path) throws ProvisionException {

        final UserImage authorImage = env.loadLatest().getUserImage(author);
        if(authorImage == null) {
            Assert.fail("No image for " + author);
        }
        final Set<String> actualPaths = authorImage.getPaths();
        if(path.length == 0) {
            if(!actualPaths.isEmpty()) {
                Assert.fail(author + " is not expected to own any path but owns " + actualPaths);
            }
            return;
        }
        final Set<String> expected;
        if(path.length == 1) {
            expected = Collections.singleton(path[0]);
        } else {
            expected = new HashSet<String>(Arrays.asList(path));
        }
        assertEquals(expected, actualPaths);
    }

    private static void assertAuthors(FSEnvironment env, String... author) throws ProvisionException {
        List<String> envAuthors = env.loadLatest().getUsers();
        if(author.length == 0) {
            if(!envAuthors.isEmpty()) {
                final StringBuilder buf = new StringBuilder("Did not expect authors: ");
                int i = envAuthors.size();
                for(String image : envAuthors) {
                    buf.append(image);
                    if(--i > 0) {
                        buf.append(", ");
                    }
                }
                Assert.fail(buf.toString());
            }
            return;
        }
        if(author.length == 1) {
            if(envAuthors.size() != 1) {
                final StringBuilder buf = new StringBuilder("Expected ");
                buf.append(author[0]);
                buf.append(" but received ");
                int i = envAuthors.size();
                for(String envAuthor : envAuthors) {
                    buf.append(envAuthor);
                    if(--i > 0) {
                        buf.append(", ");
                    }
                }
                Assert.fail(buf.toString());
            }
            assertEquals(author[0], envAuthors.get(0));
            return;
        }
        final Set<String> expected = new HashSet<String>(Arrays.asList(author));
        for(String envAuthor : envAuthors) {
            if(!expected.remove(envAuthor)) {
                Assert.fail("Author " + envAuthor + " was not expected");
            }
        }
        if(!expected.isEmpty()) {
            Assert.fail("Missing authors: " + expected);
        }
    }
}
