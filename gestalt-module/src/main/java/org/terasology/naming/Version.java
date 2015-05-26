/*
 * Copyright 2015 MovingBlocks
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

import com.google.common.base.Strings;
import org.terasology.naming.exception.VersionParseException;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern VERSION_PATTERN = Pattern.compile("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-SNAPSHOT)?");

    private int major;
    private int minor;
    private int patch;
    private boolean snapshot;

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
     * @param major The major version number (generally incremented for breaking changes)
     * @param minor The minot version number (generally changes for non-breaking feature enhancements)
     * @param patch The patch version number (generally changes for non-breaking bug fixes)
     * @param snapshot Whether this version is a snapshot (work in progress, not yet released)
     * @throws IllegalArgumentException if a version part is negative
     */
    public Version(int major, int minor, int patch, boolean snapshot) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Illegal version " + major + "." + minor + "." + patch + " - all version parts must be positive");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.snapshot = snapshot;
    }

    /**
     * @param version The string of the version
     * @throws VersionParseException If the version string is not a valid version.
     */
    public Version(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            major = Integer.parseInt(matcher.group(1));
            minor = Integer.parseInt(matcher.group(2));
            patch = Integer.parseInt(matcher.group(3));
            snapshot = !Strings.isNullOrEmpty(matcher.group(4));
        } else {
            throw new VersionParseException("Invalid version '" + version + "' - must be of the form MAJOR.minor.patch");
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    /**
     * @return Whether this version is a snapshot (work in progress)
     */
    public boolean isSnapshot() {
        return snapshot;
    }

    public Version getSnapshot() {
        if (snapshot) {
            return this;
        }
        return new Version(major, minor, patch, true);
    }

    public Version getNextMajorVersion() {
        return new Version(major + 1, 0, 0);
    }

    public Version getNextMinorVersion() {
        return new Version(major, minor + 1, 0);
    }

    public Version getNextPatchVersion() {
        return new Version(major, minor, patch + 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Version) {
            Version other = (Version) obj;
            return other.major == major && other.minor == minor && other.patch == patch && other.snapshot == snapshot;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, snapshot);
    }

    @Override
    public String toString() {
        if (isSnapshot()) {
            return major + "." + minor + "." + patch + "-SNAPSHOT";
        }
        return major + "." + minor + "." + patch;
    }

    @Override
    public int compareTo(Version other) {
        if (other.major != major) {
            return major - other.major;
        }
        if (other.minor != minor) {
            return minor - other.minor;
        }
        if (other.patch != patch) {
            return patch - other.patch;
        }
        if (other.snapshot && !snapshot) {
            return 1;
        }
        if (!other.snapshot && snapshot) {
            return -1;
        }
        return 0;
    }
}
