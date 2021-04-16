// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.assets;

import org.junit.jupiter.api.Test;
import org.terasology.gestalt.assets.exceptions.InvalidUrnException;
import org.terasology.gestalt.naming.Name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Immortius
 */
public class ResourceUrnTest {

    private static final String TEST_MODULE = "test";
    private static final String TEST_RESOURCE = "resource";
    private static final String TEST_FRAGMENT = "fragment";

    private static final String URN_STRING = TEST_MODULE + ":" + TEST_RESOURCE;
    private static final String URN_WITH_FRAGMENT_STRING = URN_STRING + "#" + TEST_FRAGMENT;
    private static final String URN_INSTANCE_STRING = TEST_MODULE + ":" + TEST_RESOURCE + "!instance";
    private static final String URN_FRAGMENT_INSTANCE_STRING = TEST_MODULE + ":" + TEST_RESOURCE + "#" + TEST_FRAGMENT + "!instance";

    @Test
    public void moduleAndResourceConstructor() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertTrue(urn.getFragmentName().isEmpty());
        assertEquals(URN_STRING, urn.toString());
    }

    @Test
    public void fragmentConstructor() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, TEST_FRAGMENT);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertEquals(new Name(TEST_FRAGMENT), urn.getFragmentName());
        assertEquals(URN_WITH_FRAGMENT_STRING, urn.toString());
    }

    @Test
    public void instanceConstructor() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, true);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertTrue(urn.isInstance());
        assertEquals(URN_INSTANCE_STRING, urn.toString());
    }

    @Test
    public void urnStringConstructor() {
        ResourceUrn urn = new ResourceUrn(URN_STRING);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertFalse(urn.isInstance());
        assertTrue(urn.getFragmentName().isEmpty());
        assertEquals(URN_STRING, urn.toString());
    }

    @Test
    public void urnWithFragmentStringConstructor() {
        ResourceUrn urn = new ResourceUrn(URN_WITH_FRAGMENT_STRING);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertEquals(new Name(TEST_FRAGMENT), urn.getFragmentName());
        assertEquals(URN_WITH_FRAGMENT_STRING, urn.toString());
    }

    @Test
    public void urnStringContructorWithInstance() {
        ResourceUrn urn = new ResourceUrn(URN_INSTANCE_STRING);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertTrue(urn.isInstance());
        assertTrue(urn.getFragmentName().isEmpty());
        assertEquals(URN_INSTANCE_STRING, urn.toString());
    }

    @Test
    public void fragmentInstanceConstructor() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, TEST_FRAGMENT, true);
        assertEquals(new Name(TEST_MODULE), urn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), urn.getResourceName());
        assertEquals(new Name(TEST_FRAGMENT), urn.getFragmentName());
        assertTrue(urn.isInstance());
        assertEquals(URN_FRAGMENT_INSTANCE_STRING, urn.toString());
    }

    @Test
    public void invalidUrnStringConstructor() {
        assertThrows(InvalidUrnException.class, () ->
                new ResourceUrn("blerg")
        );
    }

    @Test
    public void getRootResourceUrn() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, TEST_FRAGMENT);
        ResourceUrn rootUrn = urn.getRootUrn();
        assertEquals(new Name(TEST_MODULE), rootUrn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), rootUrn.getResourceName());
        assertTrue(rootUrn.getFragmentName().isEmpty());
        assertFalse(rootUrn.isInstance());
    }

    @Test
    public void getOriginResourceUrn() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, TEST_FRAGMENT, true);
        ResourceUrn parentUrn = urn.getParentUrn();
        assertEquals(new Name(TEST_MODULE), parentUrn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), parentUrn.getResourceName());
        assertEquals(new Name(TEST_FRAGMENT), parentUrn.getFragmentName());
        assertFalse(parentUrn.isInstance());
    }

    @Test
    public void getInstanceResourceUrn() {
        ResourceUrn urn = new ResourceUrn(TEST_MODULE, TEST_RESOURCE, TEST_FRAGMENT, false);
        ResourceUrn instanceUrn = urn.getInstanceUrn();
        assertEquals(new Name(TEST_MODULE), instanceUrn.getModuleName());
        assertEquals(new Name(TEST_RESOURCE), instanceUrn.getResourceName());
        assertEquals(new Name(TEST_FRAGMENT), instanceUrn.getFragmentName());
        assertTrue(instanceUrn.isInstance());
    }


}
