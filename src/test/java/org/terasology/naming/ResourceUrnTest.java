/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.naming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class ResourceUrnTest {

    private static final String TEST_MODULE = "test";
    private static final String TEST_RESOURCE = "resource";
    private static final String TEST_FRAGMENT = "fragment";

    private static final String URN_STRING = TEST_MODULE + ":" + TEST_RESOURCE;
    private static final String URN_WITH_FRAGMENT_STRING = URN_STRING + "#" + TEST_FRAGMENT;

    @Test
    public void emptyConstructor() {
        ResourceUrn urn = new ResourceUrn();
        assertTrue(urn.getModuleName().isEmpty());
        assertTrue(urn.getResourceName().isEmpty());
        assertTrue(urn.getFragmentName().isEmpty());
        assertFalse(urn.isValid());
        assertEquals("", urn.toString());
    }

    @Test
    public void moduleAndResourceConstructor() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertTrue(urn.getFragmentName().isEmpty());
        assertTrue(urn.isValid());
        assertEquals(URN_STRING, urn.toString());
    }

    @Test
    public void fragmentConstructor() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, TEST_FRAGMENT);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertEquals(new Name(TEST_FRAGMENT), urn.getFragmentName());
        assertTrue(urn.isValid());
        assertEquals(URN_WITH_FRAGMENT_STRING, urn.toString());
    }

    @Test
    public void urnStringConstructor() {
        ResourceUrn urn = new ResourceUrn(URN_STRING);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertTrue(urn.getFragmentName().isEmpty());
        assertTrue(urn.isValid());
        assertEquals(URN_STRING, urn.toString());
    }

    @Test
    public void urnWithFragmentStringConstructor() {
        ResourceUrn urn = new ResourceUrn(URN_WITH_FRAGMENT_STRING);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertEquals(new Name(TEST_FRAGMENT), urn.getFragmentName());
        assertTrue(urn.isValid());
        assertEquals(URN_WITH_FRAGMENT_STRING, urn.toString());
    }

    @Test
    public void invalidUrnStringConstructor() {
        ResourceUrn urn = new ResourceUrn("blerg");
        assertTrue(urn.getModuleName().isEmpty());
        assertTrue(urn.getResourceName().isEmpty());
        assertTrue(urn.getFragmentName().isEmpty());
        assertFalse(urn.isValid());
        assertEquals("", urn.toString());
    }

    @Test
    public void getRootResourceUrn() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, TEST_FRAGMENT);
        ResourceUrn rootUrn = urn.getRootUrn();
        assertEquals(new Name(TEST_MODULE), rootUrn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), rootUrn.getResourceName());
        assertTrue(rootUrn.getFragmentName().isEmpty());
    }
}
