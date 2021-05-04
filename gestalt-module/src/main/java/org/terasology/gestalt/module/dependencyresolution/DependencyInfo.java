// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module.dependencyresolution;

import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;
import org.terasology.gestalt.naming.VersionRange;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Describes a dependency on a module. Dependencies apply to a range of versions - anything from the min version (inclusive) to the max version (exclusive) are supported.
 *
 * @author Immortius
 */
public class DependencyInfo {

    private Name id = new Name("");
    private Version minVersion = new Version(1, 0, 0);
    private Version maxVersion;
    private boolean optional;

    public DependencyInfo() {
    }

    public DependencyInfo(DependencyInfo other) {
        this.id = other.id;
        this.minVersion = other.minVersion;
        this.maxVersion = other.maxVersion;
        this.optional = other.optional;
    }

    /**
     * @return The id of the module
     */
    public Name getId() {
        return id;
    }

    /**
     * Sets the id of the module
     *
     * @param id The id of the module
     */
    public void setId(Name id) {
        this.id = id;
    }

    /**
     * @return The minimum supported version
     */
    public Version getMinVersion() {
        return minVersion;
    }

    /**
     * The minimum supported version.
     *
     * @param value The minimum version
     */
    public void setMinVersion(Version value) {
        this.minVersion = value;
    }

    /**
     * The first unsupported version. If not explicitly specified, it is the next major version from minVersion.
     *
     * @return The maximum supported version (exclusive).
     */
    public Version getMaxVersion() {
        if (maxVersion == null) {
            if (minVersion.getMajor() == 0) {
                return minVersion.getVersionCore().getNextMinorVersion();
            }
            return minVersion.getVersionCore().getNextMajorVersion();
        }
        return maxVersion;
    }

    /**
     * The upperbound of supported versions (exclusive)
     *
     * @param value The new upperbound
     */
    public void setMaxVersion(Version value) {
        this.maxVersion = value;
    }

    /**
     * An optional dependency does not need to be present for a module with the dependency to be used. If it is present, it must fall within
     * the allowed version range.
     *
     * @return Whether this dependency is optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets whether the dependency is optional
     *
     * @param optional Whether this dependency should be optional
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Returns a predicate that yields true when applied to version that is within the version range described by this dependency information
     *
     * @return
     */
    public Predicate<Version> versionPredicate() {
        return new VersionRange(getMinVersion(), getMaxVersion());
    }

    @Override
    public String toString() {
        return String.format("DependencyInfo [id=%s, minVersion=%s, maxVersion=%s, optional=%s]",
                id, minVersion, maxVersion, optional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, minVersion, maxVersion, optional);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DependencyInfo) {
            DependencyInfo other = (DependencyInfo) obj;
            return Objects.equals(id, other.id)
                    && Objects.equals(minVersion, other.minVersion)
                    && Objects.equals(maxVersion, other.maxVersion)
                    && Objects.equals(optional, other.optional);
        }

        return false;
    }
}
