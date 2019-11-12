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

package org.terasology.gestalt.assets;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.terasology.gestalt.assets.exceptions.InvalidUrnException;
import org.terasology.gestalt.module.sandbox.API;
import org.terasology.gestalt.naming.Name;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A ResourceUrn is a urn of the structure "{moduleName}:{resourceName}[#{fragmentName}][!instance]".
 * <ul>
 * <li>moduleName is the name of the module containing or owning the resource</li>
 * <li>resourceName is the name of the resource</li>
 * <li>fragmentName is an optional identifier for a sub-part of the resource</li>
 * <li>an instance urn indicates a resource that is am independant copy of a resource identified by the rest of the urn</li>
 * </ul>
 * ResourceUrn is immutable and comparable.
 *
 * @author Immortius
 */
@API
public final class ResourceUrn implements Comparable<ResourceUrn> {

    public static final String RESOURCE_SEPARATOR = ":";
    public static final String FRAGMENT_SEPARATOR = "#";
    public static final String INSTANCE_INDICATOR = "!instance";
    private static final Pattern URN_PATTERN = Pattern.compile("([^:]+):([^#!]+)(?:#([^!]+))?(!instance)?");

    private final Name moduleName;
    private final Name resourceName;
    private final Name fragmentName;
    private final boolean instance;

    /**
     * Creates a urn with the module and resource name from the provided urn, but the fragment name provided. This urn will not be an instance urn.
     *
     * @param urn          The urn to create this urn from
     * @param fragmentName The fragment name this urn should have
     */
    public ResourceUrn(ResourceUrn urn, String fragmentName) {
        this(urn, new Name(fragmentName), false);
    }

    /**
     * Creates a urn with the module and resource name from the provided urn, but the fragment name provided. This urn will not be an instance urn.
     *
     * @param urn          The urn to create this urn from
     * @param fragmentName The fragment name this urn should have
     */
    public ResourceUrn(ResourceUrn urn, Name fragmentName) {
        this(urn, fragmentName, false);
    }

    /**
     * Creates a urn with the module and resource name from the provided urn, but the fragment name provided.
     *
     * @param urn          The urn to create this urn from
     * @param fragmentName The fragment name this urn should have
     * @param instance     Whether this urn should be a fragment
     */
    public ResourceUrn(ResourceUrn urn, String fragmentName, boolean instance) {
        this(urn, new Name(fragmentName), instance);
    }

    /**
     * Creates a urn with the module and resource name from the provided urn, but the fragment name provided.
     *
     * @param urn          The urn to create this urn from
     * @param fragmentName The fragment name this urn should have
     * @param instance     Whether this urn should be a fragment
     */
    public ResourceUrn(ResourceUrn urn, Name fragmentName, boolean instance) {
        this.moduleName = urn.getModuleName();
        this.resourceName = urn.getResourceName();
        this.fragmentName = fragmentName;
        this.instance = instance;
    }

    /**
     * Creates a ModuleUri with the given module:resource combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     */
    public ResourceUrn(String moduleName, String resourceName) {
        this(new Name(moduleName), new Name(resourceName), false);
    }

    /**
     * Creates a ModuleUri for an instance with a given module:resource(!instance) combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     * @param instance     Whether this urn identifies an instance
     */
    public ResourceUrn(String moduleName, String resourceName, boolean instance) {
        this(new Name(moduleName), new Name(resourceName), Name.EMPTY, instance);
    }

    /**
     * Creates a ModuleUri with the given module:resource combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     */
    public ResourceUrn(Name moduleName, Name resourceName) {
        this(moduleName, resourceName, Name.EMPTY, false);
    }

    /**
     * Creates a ModuleUri with the given module:resource(!instance) combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     * @param instance     Whether this urn identifies an instance
     */
    public ResourceUrn(Name moduleName, Name resourceName, boolean instance) {
        this(moduleName, resourceName, Name.EMPTY, instance);
    }

    /**
     * Creates a ModuleUri with the given module:resource#fragment combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     * @param fragmentName The name of the fragment of the resource
     */
    public ResourceUrn(String moduleName, String resourceName, String fragmentName) {
        this(new Name(moduleName), new Name(resourceName), new Name(fragmentName), false);
    }

    /**
     * Creates a ModuleUri with the given module:resource#fragment(!instance) combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     * @param fragmentName The name of the fragment of the resource
     * @param instance     Whether this urn identifies an instance
     */
    public ResourceUrn(String moduleName, String resourceName, String fragmentName, boolean instance) {
        this(new Name(moduleName), new Name(resourceName), new Name(fragmentName), instance);
    }

    /**
     * Creates a ModuleUri with the given module:resource#fragment combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     * @param fragmentName The name of the fragment of the resource
     */
    public ResourceUrn(Name moduleName, Name resourceName, Name fragmentName) {
        this(moduleName, resourceName, fragmentName, false);
    }

    /**
     * Creates a ModuleUri with the given module:resource#fragment(!instance) combo
     *
     * @param moduleName   The name of the module the resource belongs to
     * @param resourceName The name of the resource itself
     * @param fragmentName The name of the fragment of the resource
     * @param instance     Whether this urn identifies an instance
     */
    public ResourceUrn(Name moduleName, Name resourceName, Name fragmentName, boolean instance) {
        Preconditions.checkArgument(moduleName != null && !moduleName.isEmpty(), "moduleName must not be null or empty");
        Preconditions.checkArgument(resourceName != null && !resourceName.isEmpty(), "resourceName must not be null or empty");
        this.moduleName = moduleName;
        this.resourceName = resourceName;
        this.fragmentName = fragmentName;
        this.instance = instance;
    }

    /**
     * Creates a ModuleUrn from a string in the format "module:object(#fragment)(!instance)".
     *
     * @param urn The urn to parse
     * @throws org.terasology.gestalt.assets.exceptions.InvalidUrnException if the string is not a valid resource urn
     */
    public ResourceUrn(String urn) {
        Matcher match = URN_PATTERN.matcher(urn);
        if (match.matches()) {
            moduleName = new Name(match.group(1));
            resourceName = new Name(match.group(2));
            if (!Strings.isNullOrEmpty(match.group(3))) {
                fragmentName = new Name(match.group(3));
            } else {
                fragmentName = Name.EMPTY;
            }
            instance = !Strings.isNullOrEmpty(match.group(4));
        } else {
            throw new InvalidUrnException("Invalid Urn: '" + urn + "'");
        }
    }

    /**
     * @param urn The string to check for validity
     * @return Whether urn is a valid ResourceUrn
     */
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
     * @return The resource name part of the urn. This identifies the resource itself.
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
     * @return Whether this urn identifies an independent copy of the resource
     */
    public boolean isInstance() {
        return instance;
    }

    /**
     * @return The root of the ResourceUrn, without the fragment name or instance marker.
     */
    public ResourceUrn getRootUrn() {
        if (fragmentName.isEmpty() && !isInstance()) {
            return this;
        }
        return new ResourceUrn(moduleName, resourceName);
    }

    /**
     * @return If this urn is an instance, returns the urn without the instance marker. Otherwise this urn.
     */
    public ResourceUrn getParentUrn() {
        if (isInstance()) {
            return new ResourceUrn(moduleName, resourceName, fragmentName);
        } else {
            return this;
        }
    }

    /**
     * @return This instance urn version of this urn. If this urn is already an instance, this urn is returned.
     */
    public ResourceUrn getInstanceUrn() {
        if (!isInstance()) {
            return new ResourceUrn(moduleName, resourceName, fragmentName, true);
        } else {
            return this;
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(moduleName);
        stringBuilder.append(RESOURCE_SEPARATOR);
        stringBuilder.append(resourceName);
        if (!fragmentName.isEmpty()) {
            stringBuilder.append(FRAGMENT_SEPARATOR);
            stringBuilder.append(fragmentName);
        }
        if (instance) {
            stringBuilder.append(INSTANCE_INDICATOR);
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResourceUrn) {
            ResourceUrn other = (ResourceUrn) obj;
            return Objects.equal(moduleName, other.moduleName) && Objects.equal(resourceName, other.resourceName)
                    && Objects.equal(fragmentName, other.fragmentName) && instance == other.instance;
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
        if (result == 0) {
            if (instance && !o.instance) {
                result = 1;
            } else if (!instance && o.instance) {
                result = -1;
            }
        }
        return result;
    }


}
