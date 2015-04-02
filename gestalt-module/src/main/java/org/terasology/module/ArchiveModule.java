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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.terasology.module.exceptions.InvalidModulePathException;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * A module that is a archive (zip or jar) file that exists on the file system, but outside the classpath.
 *
 * @author Immortius
 */
public class ArchiveModule extends BaseModule {
    private final ImmutableList<URL> classpath;

    /**
     * @param path     Must be a file, and must be convertible to a url (some limits on special characters)
     * @param metadata The metadata describing this module
     */
    public ArchiveModule(Path path, ModuleMetadata metadata) {
        super(Arrays.asList(path), metadata);
        Preconditions.checkArgument(Files.isRegularFile(path));
        try {
            classpath = ImmutableList.of(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new InvalidModulePathException("Could not convert path to URL: " + path, e);
        }
    }

    @Override
    public ImmutableList<URL> getClasspaths() {
        return classpath;
    }

    @Override
    public boolean isOnClasspath() {
        return false;
    }

    @Override
    public boolean isCodeModule() {
        return true;
    }

}
