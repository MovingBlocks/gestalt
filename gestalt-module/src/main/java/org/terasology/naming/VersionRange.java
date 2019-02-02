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

import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * A range of versions from a lower-bound (inclusive) to an upper-bound (exclusive).
 *
 * @author Immortius
 */
public class VersionRange {
    private final Version lowerBound;
    private final Version upperBound;

    /**
     * lowerBound must be less than or equal to upperBound
     *
     * @param lowerBound The lower bound of the range. Cannot be null
     * @param upperBound The upper bound of the range. Cannot be null
     */
    public VersionRange(Version lowerBound, Version upperBound) {
        Preconditions.checkNotNull(lowerBound);
        Preconditions.checkNotNull(upperBound);
        Preconditions.checkArgument(lowerBound.compareTo(upperBound) < 0, "upperBound must be greater than lowerBound");
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public Version getLowerBound() {
        return lowerBound;
    }

    public Version getUpperBound() {
        return upperBound;
    }

    /**
     * @param version The version to check
     * @return Whether version falls within the range
     */
    public boolean contains(Version version) {
        return version.compareTo(lowerBound.getSnapshot()) >= 0 && version.compareTo(upperBound.getSnapshot()) < 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof VersionRange) {
            VersionRange other = (VersionRange) obj;
            return Objects.equals(lowerBound, other.lowerBound) && Objects.equals(upperBound, other.upperBound);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound);
    }

    @Override
    public String toString() {
        return "[" + lowerBound + "," + upperBound + ")";
    }

}
