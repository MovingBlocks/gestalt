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

import com.google.common.base.Preconditions;

import java.util.Locale;

import javax.annotation.concurrent.Immutable;

/**
 * A name is a normalised string used as an identifier. Primarily this means it is case insensitive.
 * <p>
 * The original case-sensitive name is retained and available for display purposes, since it may use camel casing for readability.
 * </p><p>
 * This class is immutable.
 * </p>
 *
 * @author Immortius
 */
@Immutable
public final class Name implements Comparable<Name> {
    /**
     * The Name equivalent of an empty String
     */
    public static final Name EMPTY = new Name("");

    private final String normalizedName;
    private final String originalName;

    public Name(String name) {
        Preconditions.checkNotNull(name);
        this.originalName = name;
        this.normalizedName = name.toLowerCase(Locale.ENGLISH);
    }

    /**
     * @return The original <b>case-sensitive</b>  name when string is passed into name
     */
    public String displayName() {
        return originalName;
    }

    /**
     * @return Whether this name is empty (equivalent to an empty string)
     */
    public boolean isEmpty() {
        return normalizedName.isEmpty();
    }

    /**
     * @return The Name in lowercase consistent with Name equality (so two names that are equal will have the same lowercase)
     * @deprecated This is scheduled for removal in upcoming versions. 
     *             Use {@code toString} or {@code displayName} instead.
     *             Note that a Name should not be transformed to a String for further processing.
     */
    @Deprecated
    public String toLowerCase() {
        return normalizedName;
    }

    /**
     * @return The Name in uppercase consistent with Name equality (so two names that are equal will have the same uppercase)
     * @deprecated
     */
    public String toUpperCase() {
        return originalName.toUpperCase(Locale.ENGLISH);
    }

    @Override
    public int compareTo(Name o) {
        return normalizedName.compareTo(o.normalizedName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Name) {
            Name other = (Name) obj;
            return normalizedName.equals(other.normalizedName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return normalizedName.hashCode();
    }

    @Override
    public String toString() {
        return normalizedName;
    }
}
