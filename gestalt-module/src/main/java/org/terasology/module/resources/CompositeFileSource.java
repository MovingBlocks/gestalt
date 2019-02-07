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

package org.terasology.module.resources;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.terasology.util.Varargs;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CompositeFileSource combines multiple ModuleFileSources together to act as a single ModuleFileSource
 */
public class CompositeFileSource implements ModuleFileSource {

    private final List<ModuleFileSource> sources;

    /**
     * @param source  The first (mandatory) file source
     * @param sources Any additional file sources
     */
    public CompositeFileSource(ModuleFileSource source, ModuleFileSource... sources) {
        this.sources = Varargs.combineToList(source, sources);
    }

    /**
     * @param sources A non-empty list of file sources
     */
    public CompositeFileSource(List<ModuleFileSource> sources) {
        this.sources = ImmutableList.copyOf(sources);
    }

    @Override
    public Optional<ModuleFile> getFile(List<String> filepath) {
        for (ModuleFileSource source : sources) {
            Optional<ModuleFile> result = source.getFile(filepath);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<ModuleFile> getFiles() {
        return sources.stream().flatMap(x -> x.getFiles().stream()).collect(Collectors.toList());
    }

    @Override
    public Collection<ModuleFile> getFilesInPath(boolean recursive, List<String> path) {
        return sources.stream().flatMap(x -> x.getFilesInPath(recursive, path).stream()).collect(Collectors.toList());
    }

    @Override
    public Set<String> getSubpaths(List<String> fromPath) {
        return sources.stream().flatMap(x -> x.getSubpaths(fromPath).stream()).collect(Collectors.toSet());
    }

    @NonNull
    @Override
    public Iterator<ModuleFile> iterator() {
        return sources.stream().flatMap(x -> x.getFiles().stream()).iterator();
    }
}
