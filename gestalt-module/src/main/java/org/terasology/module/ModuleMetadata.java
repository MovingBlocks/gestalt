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

package org.terasology.module;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.terasology.i18n.I18nMap;
import org.terasology.module.dependencyresolution.DependencyInfo;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Information describing a module.
 *
 * @author Immortius
 */
public class ModuleMetadata {

    /*
     * Constants for the names of each of the core metadata attributes.
     */
    public static final String ID = "id";
    public static final String VERSION = "version";
    public static final String DISPLAY_NAME = "displayName";
    public static final String DESCRIPTION = "description";
    public static final String DEPENDENCIES = "dependencies";
    public static final String REQUIRED_PERMISSIONS = "requiredPermissions";

    /**
     * The set of reserved ids that cannot be used by extensions.
     */
    public static final Set<String> RESERVED_IDS = ImmutableSet.of(ID, VERSION, DISPLAY_NAME, DESCRIPTION, DEPENDENCIES, REQUIRED_PERMISSIONS);
    private final Map<String, Object> extensions = Maps.newHashMap();
    private Name id;
    private Version version = Version.DEFAULT;
    private I18nMap displayName = new I18nMap("");
    private I18nMap description = new I18nMap("");
    private Set<String> requiredPermissions = Sets.newLinkedHashSet();
    private List<DependencyInfo> dependencies = Lists.newArrayList();

    public ModuleMetadata() {
    }

    /**
     * @return The identifier of the module
     */
    public Name getId() {
        return id;
    }

    /**
     * Sets the identifier of the module
     *
     * @param id The new identifier
     */
    public void setId(Name id) {
        this.id = id;
    }

    /**
     * @return The version of the module
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Sets the version of the module
     *
     * @param version The new version
     */
    public void setVersion(Version version) {
        this.version = version;
    }

    /**
     * @return A displayable name of the module
     */
    public I18nMap getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName The new human-readable name of the module
     */
    public void setDisplayName(I18nMap displayName) {
        this.displayName = displayName;
    }

    /**
     * @return A human readable description of the module
     */
    public I18nMap getDescription() {
        return description;
    }

    /**
     * @param description The new human-readable description of the module
     */
    public void setDescription(I18nMap description) {
        this.description = description;
    }

    /**
     * @return A list of the permissions required by this module, corresponding to permission sets installed in the security manager.
     */
    public Set<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    /**
     * @return A list of dependencies of the module
     */
    public List<DependencyInfo> getDependencies() {
        if (dependencies == null) {
            dependencies = Lists.newArrayList();
        }
        return dependencies;
    }

    /**
     * @param dependencyId The id of the module to get dependency information on
     * @return The depdendency information for a specific module, or null if no such dependency exists
     */
    public DependencyInfo getDependencyInfo(Name dependencyId) {
        for (DependencyInfo dependency : dependencies) {
            if (Objects.equals(dependencyId, dependency.getId())) {
                return dependency;
            }
        }
        return null;
    }

    /**
     * @param extensionId  The identifier of the extension
     * @param expectedType The expected type of the extension
     * @param <T>          The expected type of the extension
     * @return The extension object, or null if it is missing or of an incompatible type
     */
    public <T> T getExtension(String extensionId, Class<T> expectedType) {
        Object extension = extensions.get(extensionId);
        if (expectedType.isInstance(extension)) {
            return expectedType.cast(extension);
        }
        return null;
    }

    /**
     * Sets the value of an extension
     *
     * @param extensionId The identifier of the extension
     * @param extension   The extension object
     * @throws IllegalArgumentException if extensionId is a ReservedIds
     */
    public void setExtension(String extensionId, Object extension) {
        Preconditions.checkArgument(!RESERVED_IDS.contains(extensionId), "Reserved id '" + extensionId + "' cannot be used to identify an extension");
        extensions.put(extensionId, extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, displayName, description, requiredPermissions, dependencies, extensions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ModuleMetadata) {
            ModuleMetadata other = (ModuleMetadata) obj;

            return Objects.equals(id, other.id)
                    && Objects.equals(version, other.version)
                    && Objects.equals(displayName, other.displayName)
                    && Objects.equals(description, other.description)
                    && Objects.equals(requiredPermissions, other.requiredPermissions)
                    && Objects.equals(dependencies, other.dependencies)
                    && Objects.equals(extensions, other.extensions);
        }
        return false;
    }
}
