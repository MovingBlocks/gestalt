// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module.dependencyresolution;

import org.junit.jupiter.api.Test;
import org.terasology.gestalt.naming.Version;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInfoTest {

    @Test
    void testMinimumVersionUnderOnePointZero() {
        Version minVersion = new Version("0.2.4");

        DependencyInfo dependency = new DependencyInfo();
        dependency.setMinVersion(minVersion);
        Predicate<Version> predicate = dependency.versionPredicate();

        assertTrue(predicate.test(minVersion), "failed to match its own version");

        Version nextVersion = minVersion.getNextPatchVersion();
        assertTrue(predicate.test(nextVersion), "failed to match the next patch version");
        assertTrue(predicate.test(new Version(nextVersion + "-SNAPSHOT")), "failed to match the next snapshot");

        assertFalse(predicate.test(new Version("0.2.3")), "inappropriately matched an earlier patch version");

        assertFalse(predicate.test(nextVersion.getNextMinorVersion()), "inappropriately matched next 0.x minor version");
    }

    @Test
    void testMinimumVersionOverOnePointZero() {
        Version minVersion = new Version("1.4.7");

        DependencyInfo dependency = new DependencyInfo();
        dependency.setMinVersion(minVersion);
        Predicate<Version> predicate = dependency.versionPredicate();

        assertTrue(predicate.test(minVersion), "failed to match its own version");

        Version nextVersion = minVersion.getNextPatchVersion();
        assertTrue(predicate.test(nextVersion), "failed to match the next patch version");
        assertTrue(predicate.test(new Version(nextVersion + "-SNAPSHOT")), "failed to match the next snapshot");

        Version nextMinorVersion = minVersion.getNextMinorVersion();
        assertTrue(predicate.test(nextMinorVersion), "failed to match the next minor version");
        assertTrue(predicate.test(new Version(nextMinorVersion + "-SNAPSHOT")), "failed to match the next minor snapshot");

        assertFalse(predicate.test(new Version("1.4.6")), "inappropriately matched an earlier patch version");
        assertFalse(predicate.test(nextVersion.getNextMajorVersion()), "inappropriately matched next major version");
    }

    @Test
    void testMinimumVersionIsSnapshot() {
        Version snapshot = new Version("2.0.0-SNAPSHOT");
        Version release = new Version("2.0.0");

        DependencyInfo dependency = new DependencyInfo();
        dependency.setMinVersion(snapshot);
        Predicate<Version> predicate = dependency.versionPredicate();

        assertTrue(predicate.test(snapshot), "failed to match its own version");
        assertTrue(predicate.test(release), "failed to match release version");

        Version nextVersion = release.getNextPatchVersion();
        assertTrue(predicate.test(nextVersion), "failed to match the next patch version");
        assertTrue(predicate.test(new Version(nextVersion + "-SNAPSHOT")), "failed to match the next snapshot");
        assertFalse(predicate.test(release.getNextMajorVersion()), "inappropriately matched next major version");
    }
}
