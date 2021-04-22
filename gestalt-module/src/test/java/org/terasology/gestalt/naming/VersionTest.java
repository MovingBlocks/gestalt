// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.naming;

import org.junit.jupiter.api.Test;
import org.terasology.gestalt.naming.exception.VersionParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Immortius
 */
public class VersionTest {

    @Test
    public void parseVersion() {
        Version version = new Version("1.2.3");
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertFalse(version.isSnapshot());
    }

    @Test
    public void parseSnapshotVersion() {
        Version version = new Version("1.2.3-SNAPSHOT");
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertTrue(version.isSnapshot());
        assertEquals("1.2.3-SNAPSHOT", version.toString());
    }

    @Test
    public void snapshotVersionLessThanRealVersion() {
        Version snapshot = new Version("1.2.3-SNAPSHOT");
        Version real = new Version("1.2.3");
        assertNotEquals(snapshot, real);
        assertTrue(snapshot.compareTo(real) < 0);
    }

    @Test
    public void getNextMajorVersion() {
        Version version = new Version(1, 2, 3);
        assertEquals(new Version(2, 0, 0), version.getNextMajorVersion());
    }

    @Test
    public void getNextMinorVersion() {
        Version version = new Version(1, 2, 3);
        assertEquals(new Version(1, 3, 0), version.getNextMinorVersion());
    }

    @Test
    public void getNextPatchVersion() {
        Version version = new Version(1, 2, 3);
        assertEquals(new Version(1, 2, 4), version.getNextPatchVersion());
    }

    @Test
    public void exceptionCreatingWithNegativeMajorVersion() {
        assertThrows(IllegalArgumentException.class, () ->
                new Version(-1, 0, 0)
        );
    }

    @Test
    public void exceptionCreatingWithNegativeMinorVersion() {
        assertThrows(IllegalArgumentException.class, () ->
                new Version(0, -1, 0)
        );
    }

    @Test
    public void exceptionCreatingWithNegativePatchVersion() {
        assertThrows(IllegalArgumentException.class, () ->
                new Version(0, 0, -1)
        );
    }

    @Test
    public void exceptionParsingMalformedString() {
        assertThrows(VersionParseException.class, () ->
                new Version("hello")
        );
    }

    @Test
    public void exceptionParsingMalformedSeparator() {
        assertThrows(VersionParseException.class, () ->
                new Version("1,2,3")
        );
    }

    @Test
    public void exceptionParsingInvalidNumbers() {
        assertThrows(VersionParseException.class, () ->
                new Version("1.1.6a")
        );
    }

    @Test
    public void compareVersions() {
        assertEquals(0, new Version(1, 0, 0).compareTo(new Version(1, 0, 0)));
        assertEquals(-1, new Version(1, 0, 0).compareTo(new Version(2, 0, 0)));
        assertEquals(1, new Version(2, 0, 0).compareTo(new Version(1, 0, 0)));
        assertEquals(1, new Version(1, 1, 0).compareTo(new Version(1, 0, 0)));
        assertEquals(-1, new Version(1, 0, 0).compareTo(new Version(1, 1, 0)));
        assertEquals(1, new Version(1, 0, 1).compareTo(new Version(1, 0, 0)));
        assertEquals(-1, new Version(1, 0, 0).compareTo(new Version(1, 0, 1)));
    }

    @Test
    public void versionToString() {
        assertEquals("1.2.3", new Version(1, 2, 3).toString());
    }

}
