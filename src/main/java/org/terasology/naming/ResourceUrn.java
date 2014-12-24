/*
 * Copyright 2014 MovingBlocks
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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.terasology.naming.exceptions.InvalidUrnException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A ResourceUrn is a urn of the structure "{moduleName}:{resourceName}[#{fragmentName}]".
 * <ul>
 * <li>moduleName is the name of the module containing or owning the resource</li>
 * <li>resourceName is the name of the resource</li>
 * <li>fragmentName is an optional identifier for a sub-part of the resource</li>
 * </ul>
 * ResourceUrn is immutable and comparable.
 *
 * @author Immortius
 */
public final class ResourceUrn implements Comparable<ResourceUrn> {

    private static final String RESOURCE_SEPARATOR = ":";
    private static final String FRAGMENT_SEPARATOR = "#";
    private static final Pattern URN_PATTERN = Pattern.compile("([^:#]+):([^#]+)(?:#([^#]+))?");

    private Name moduleName = Name.EMPTY;
    private Name resourceName = Name.EMPTY;
    private Name fragmentName = Name.EMPTY;


    /**
     * Creates a ModuleUri for the given module:resource combo
     *
     * @param moduleName
     * @param resourceName
     */
    public ResourceUrn(String moduleName, String resourceName) {
        this(new Name(moduleName), new Name(resourceName));
    }

    /**
     * Creates a ModuleUri for the given module:resource combo
     *
     * @param moduleName
     * @param resourceName
     */
    public ResourceUrn(Name moduleName, Name resourceName) {
        Preconditions.checkArgument(moduleName != null && !moduleName.isEmpty(), "moduleName must not be null or empty");
        Preconditions.checkArgument(resourceName != null && !resourceName.isEmpty(), "resourceName must not be null or empty");
        this.moduleName = moduleName;
        this.resourceName = resourceName;
    }

    /**
     * Creates a ModuleUri for the given module:resource#fragment combo
     *
     * @param moduleName
     * @param resourceName
     * @param fragmentName
     */
    public ResourceUrn(String moduleName, String resourceName, String fragmentName) {
        this(new Name(moduleName), new Name(resourceName), new Name(fragmentName));
    }

    /**
     * Creates a ModuleUri for the given module:resource#fragment combo
     *
     * @param moduleName
     * @param resourceName
     * @param fragmentName
     */
    public ResourceUrn(Name moduleName, Name resourceName, Name fragmentName) {
        Preconditions.checkArgument(moduleName != null && !moduleName.isEmpty(), "moduleName must not be null or empty");
        Preconditions.checkArgument(resourceName != null && !resourceName.isEmpty(), "resourceName must not be null or empty");
        Preconditions.checkArgument(fragmentName != null && !fragmentName.isEmpty(), "fragmentName must not be null or empty");
        this.moduleName = moduleName;
        this.resourceName = resourceName;
        this.fragmentName = fragmentName;
    }

    /**
     * Creates a ModuleUrn from a string in the format "module:object(#fragment)". If the string does not match this format, an invalid urn is returned.
     *
     * @param urn
     * @throws org.terasology.naming.exceptions.InvalidUrnException
     */
    public ResourceUrn(String urn) {
        Matcher match = URN_PATTERN.matcher(urn);
        if (match.matches()) {
            moduleName = new Name(match.group(1));
            resourceName = new Name(match.group(2));
            if (!Strings.isNullOrEmpty(match.group(3))) {
                fragmentName = new Name(match.group(3));
            }
        } else {
            throw new InvalidUrnException("Invalid Urn: '" + urn + "'");
        }
    }

    public static boolean isValid(String urn) {
        return URN_PATTERN.matcher(urn).matches();
    }

    /**
     * @return The module name part of the urn. This identifies the module that the resource belongs to.
     */
    public Name getModuleName() {
        return moduleName;
    }

    /**
     * @return The resource name part of the urn. This identifies the module itself.
     */
    public Name getResourceName() {
        return resourceName;
    }

    /**
     * @return The fragment name part of the urn. This identifies a sub-part of the resource.
     */
    public Name getFragmentName() {
        return fragmentName;
    }

    /**
     * @return The root of the ResourceUrn, without the fragment name.
     */
    public ResourceUrn getRootUrn() {
        if (fragmentName.isEmpty()) {
            return this;
        }
        return new ResourceUrn(moduleName, resourceName);
    }

    @Override
    public String toString() {
        if (fragmentName.isEmpty()) {
            return moduleName + RESOURCE_SEPARATOR + resourceName;
        } else {
            return moduleName + RESOURCE_SEPARATOR + resourceName + FRAGMENT_SEPARATOR + fragmentName;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResourceUrn) {
            ResourceUrn other = (ResourceUrn) obj;
            return Objects.equal(moduleName, other.moduleName) && Objects.equal(resourceName, other.resourceName) && Objects.equal(fragmentName, other.fragmentName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fragmentName, resourceName, moduleName);
    }

    @Override
    public int compareTo(ResourceUrn o) {
        int result = moduleName.compareTo(o.getModuleName());
        if (result == 0) {
            result = resourceName.compareTo(o.getResourceName());
        }
        if (result == 0) {
            result = fragmentName.compareTo(o.getFragmentName());
        }
        return result;
    }
}
