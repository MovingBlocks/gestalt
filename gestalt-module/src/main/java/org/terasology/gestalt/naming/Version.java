/*
 * Copyright 2019 MovingBlocks
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
package org.terasology.gestalt.naming;

import com.github.zafarkhaja.semver.ParseException;
import org.terasology.gestalt.naming.exception.VersionParseException;

/**
 * Wrapper for a semantic version string - for version numbers of the form MAJOR.minor.patch(-SNAPSHOT). Allows the individual
 * elements to be retrieved, and for comparison between versions.
 *
 * @author Immortius
 */
public final class Version implements Comparable<Version> {
    /**
     * A default version of 1.0.0
     */
    public static final Version DEFAULT = new Version(1, 0, 0);

    private static final String SNAPSHOT = "SNAPSHOT";

    private com.github.zafarkhaja.semver.Version semver;

    /**
     * Constructs a version with the given values
     *
     * @param major The major version number (generally incremented for breaking changes)
     * @param minor The minot version number (generally changes for non-breaking feature enhancements)
     * @param patch The patch version number (generally changes for non-breaking bug fixes)
     * @throws IllegalArgumentException if a version part is negative
     */
    public Version(int major, int minor, int patch) {
        this(major, minor, patch, false);
    }

    /**
     * Constructs a version with the given values
     *
     * @param major    The major version number (generally incremented for breaking changes)
     * @param minor    The minot version number (generally changes for non-breaking feature enhancements)
     * @param patch    The patch version number (generally changes for non-breaking bug fixes)
     * @param snapshot Whether this version is a snapshot (work in progress, not yet released)
     * @throws IllegalArgumentException if a version part is negative
     */
    public Version(int major, int minor, int patch, boolean snapshot) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Illegal version " + major + "." + minor + "." + patch + " - all version parts must be positive");
        }


        final com.github.zafarkhaja.semver.Version baseVersion = com.github.zafarkhaja.semver.Version.of(major, minor, patch);
        this.semver = snapshot ? baseVersion.setPreReleaseVersion(SNAPSHOT) : baseVersion;
    }

    /**
     * Constructor.
     * @param version The string of the version
     * @throws VersionParseException If the version string is not a valid version.
     */
    public Version(String version) {
        try {
            this.semver = com.github.zafarkhaja.semver.Version.parse(version);
        } catch (ParseException e) {
            throw new VersionParseException("Invalid version '" + version + "' - must be of the form MAJOR.minor.patch");
        }
    }

    /**
     * Create a new Version from the internal representation.
     *
     * @param semver the internal representation of a semantic version
     */
    private Version(com.github.zafarkhaja.semver.Version semver) {
        this.semver = semver;
    }

    /**
     * Returns the major version.
     * @return i.e. 5 out of 5.1.3
     */
    public int getMajor() {
        return (int) semver.majorVersion();
    }

    /**
     * Returns the minor version.
     * @return i.e. 1 out of 5.1.3
     */
    public int getMinor() {
        return (int) semver.minorVersion();
    }

    /**
     * Returns the patch version.
     * @return i.e. 3 out of 5.1.3
     */
    public int getPatch() {
        return (int) semver.patchVersion();
    }

    /**
     * Whether this version is a snapshot (work in progress)
     * @return true if this version is a snapshot
     */
    public boolean isSnapshot() {
        return !semver.preReleaseVersion().isEmpty();
    }

    /**
     * Gets current snapshot version.
     * @return the snapshot version
     */
    public Version getSnapshot() {
        return new Version(semver.setPreReleaseVersion(SNAPSHOT));
    }

    /**
     * Gets next major version.
     * @return  i.e. 6 if version is 5.1.3
     */
    public Version getNextMajorVersion() {
        return new Version(semver.nextMajorVersion());
    }


    /**
     * Gets next minor version.
     * @return  i.e. 2 if version is 5.1.3
     */
    public Version getNextMinorVersion() {
        return new Version(semver.nextMinorVersion());
    }

    /**
     * Gets next patch version.
     * @return  i.e. 4 if version is 5.1.3
     */
    public Version getNextPatchVersion() {
        return new Version(semver.nextPatchVersion());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Version) {
            Version other = (Version) obj;
            return this.semver.equals(other.semver);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.semver.hashCode();
    }

    @Override
    public String toString() {
        return this.semver.toString();
    }

    @Override
    public int compareTo(Version other) {
        return this.semver.compareTo(other.semver);
    }
}
