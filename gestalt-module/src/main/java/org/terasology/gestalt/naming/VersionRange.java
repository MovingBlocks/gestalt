// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.naming;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A range of versions from a lower-bound (inclusive) to an upper-bound (exclusive).
 *
 * @author Immortius
 */
public class VersionRange implements Predicate<Version> {
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
        return version.compareTo(lowerBound) >= 0 && version.compareTo(upperBound.getSnapshot()) < 0;
    }

    @Override
    public boolean test(Version version) {
        return contains(version);
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
