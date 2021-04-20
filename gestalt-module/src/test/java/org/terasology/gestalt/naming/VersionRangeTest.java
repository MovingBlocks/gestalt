// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Immortius
 */
public class VersionRangeTest {

    private VersionRange range = new VersionRange(new Version("1.2.3"), new Version("2.3.4"));

    @Test
    public void doesNotContainLowerVersion() {
        assertFalse(range.contains(new Version("1.0.0")));
        assertFalse(range.contains(new Version("1.2.2")));
        assertFalse(range.contains(new Version("1.1.5")));
    }

    @Test
    public void containsLowerBound() {
        assertTrue(range.contains(new Version("1.2.3")));
    }

    @Test
    public void containsVersions() {
        assertTrue(range.contains(new Version("1.6.0")));
        assertTrue(range.contains(new Version("2.0.0")));
        assertTrue(range.contains(new Version("2.3.3")));
    }

    @Test
    public void doesNotContainUpperBound() {
        assertFalse(range.contains(new Version("2.3.4")));
    }

    @Test
    public void doesNotContainHigherVersions() {
        assertFalse(range.contains(new Version("2.3.10")));
        assertFalse(range.contains(new Version("2.4.0")));
        assertFalse(range.contains(new Version("3.0.0")));
    }

    @Test
    public void errorIfUpperBoundLowerThanLowerBound() {
        assertThrows(IllegalArgumentException.class, () ->
                new VersionRange(new Version("2.0.0"), new Version("1.0.0"))
        );
    }

    @Test
    public void lowerSnapshotInRange() {
        assertTrue(range.contains(new Version("1.2.3-SNAPSHOT")));
    }

    @Test
    public void higherSnapshotOutOfRange() {
        assertFalse(range.contains(new Version("2.3.4-SNAPSHOT")));
    }

    @Test
    public void canMatchMajorSnapshot() {
        Version majorSnapshot = new Version("2.0.0-SNAPSHOT");
        Version majorRelease = new Version("2.0.0");

        VersionRange snapshotRange = new VersionRange(majorSnapshot, majorRelease);
        assertTrue(snapshotRange.contains(majorSnapshot));
    }
}
