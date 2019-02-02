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

package org.terasology.module.dependencyresolution;

import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.naming.VersionRange;

import java.util.Objects;

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
                return minVersion.getNextMinorVersion();
            }
            return minVersion.getNextMajorVersion();
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
     * @return The range of supported versions
     */
    public VersionRange versionRange() {
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
