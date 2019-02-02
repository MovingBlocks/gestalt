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
package org.terasology.naming;

import java.util.Objects;

/**
 * A combined name and version, for uniquely identifying something by Name and Version.
 * <p>
 * Immutable.
 * </p>
 *
 * @author Immortius
 */
public class NameVersion {
    private final Name name;
    private final Version version;

    public NameVersion(Name name, Version version) {
        this.name = name;
        this.version = version;
    }

    public Name getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return name + ":" + version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NameVersion) {
            NameVersion other = (NameVersion) obj;
            return Objects.equals(name, other.name) && Objects.equals(version, other.version);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, name);
    }
}
