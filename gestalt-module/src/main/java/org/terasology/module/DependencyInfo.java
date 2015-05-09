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
package org.terasology.module;

import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.naming.VersionRange;

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
     * If this dependency is optional.
     * @return
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets the optional flag for a dependency.
     * @param optional If dependency should be optional.
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * @return The id of the module
     */
    public Name getId() {
        return id;
    }

    /**
     * @return The minimum supported version
     */
    public Version getMinVersion() {
        return minVersion;
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
     * @return The range of supported versions
     */
    public VersionRange versionRange() {
        return new VersionRange(getMinVersion(), getMaxVersion());
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
     * The minimum supported version.
     *
     * @param value The minimum version
     */
    public void setMinVersion(Version value) {
        this.minVersion = value;
    }

    /**
     * The upperbound of supported versions (exclusive)
     *
     * @param value The new upperbound
     */
    public void setMaxVersion(Version value) {
        this.maxVersion = value;
    }
}
