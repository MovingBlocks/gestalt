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
import com.google.common.collect.ImmutableSet;
import org.reflections.Reflections;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.net.URL;
import java.nio.file.Path;

/**
 * A module is an identified and versioned set of code and/or resources that can be loaded and used at runtime. This class encapsulates information on a
 * module.
 *
 * @author Immortius
 */
public interface Module {

    /**
     * @return The locations composing the module
     */
    ImmutableList<Path> getLocations();

    /**
     * @return The urls forming the classpath of the module
     */
    ImmutableList<URL> getClasspaths();

    /**
     * @return The identifier for the module
     */
    Name getId();

    /**
     * @return The version of the module
     */
    Version getVersion();

    /**
     * @return The list of permission sets required by this module
     */
    ImmutableSet<String> getRequiredPermissions();

    /**
     * Whether the module is included in the classpath of the application. These are not loaded dynamically and hence are not sandboxed and are always active.
     *
     * @return Whether this module is on the classpath
     */
    boolean isOnClasspath();

    /**
     * @return Whether the module may introduce code elements
     */
    boolean isCodeModule();

    /**
     * Provides the partial reflection information for this module, in isolation of other modules.  This information is of limited use by itself - without combining
     * it with the information from its dependencies, it will be unable to resolve subtypes if an intermediate class is missing. Discovered classes will also not be
     * instantiable.
     * <p/>
     * Intended for use in building a reflection information for a complete environment.
     *
     * @return The partial reflection information for this module in isolation
     */
    Reflections getReflectionsFragment();

    /**
     * @return Metadata describing the module
     */
    ModuleMetadata getMetadata();

}
