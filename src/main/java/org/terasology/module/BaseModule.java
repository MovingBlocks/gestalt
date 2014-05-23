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

package org.terasology.module;

import com.google.common.collect.ImmutableList;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Common base class for modules that live on the file system.
 *
 * @author Immortius
 */
public abstract class BaseModule implements Module {
    protected Collection<Path> paths;
    protected ModuleMetadata metadata;

    /**
     * @param paths The paths composing the module
     * @param metadata The metadata describing the module
     */
    public BaseModule(Collection<Path> paths, ModuleMetadata metadata) {
        this.paths = ImmutableList.copyOf(paths);
        this.metadata = metadata;
    }

    @Override
    public Collection<Path> getLocations() {
        return paths;
    }

    @Override
    public Name getId() {
        return metadata.getId();
    }

    @Override
    public Version getVersion() {
        return metadata.getVersion();
    }

    @Override
    public ModuleMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof BaseModule) {
            BaseModule other = (BaseModule) obj;
            return Objects.equals(other.getId(), getId()) && Objects.equals(other.getVersion(), getVersion());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getVersion());
    }

    @Override
    public String toString() {
        return metadata.getId() + "-" + metadata.getVersion();
    }
}
