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

package org.terasology.assets.module;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetInput;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author Immortius
 */
class UnloadedAsset<T extends AssetData> {
    private static final Logger logger = LoggerFactory.getLogger(UnloadedAsset.class);

    private final ResourceUrn urn;
    private final ModuleEnvironment environment;
    private final List<Source<AssetFormat<T>>> sources = Lists.newArrayList();
    private final List<Source<AssetAlterationFormat<T>>> supplementSources = Lists.newArrayList();
    private final List<Source<AssetAlterationFormat<T>>> deltaSources = Lists.newArrayList();

    public UnloadedAsset(ResourceUrn urn, ModuleEnvironment environment) {
        this.urn = urn;
        this.environment = environment;
    }

    public ResourceUrn getUrn() {
        return urn;
    }

    public boolean isValid() {
        return !sources.isEmpty();
    }

    public boolean addSource(Name providingModule, AssetFormat<T> format, Path input) {
        if (!providingModule.equals(urn.getModuleName()) && !environment.getDependencyNamesOf(providingModule).contains(urn.getModuleName())) {
            logger.warn("Module '{}' provides override for non-dependency '{}' - {}", providingModule, urn.getModuleName(), urn);
            return false;
        } else {
            sources.add(new Source<>(providingModule, format, new AssetInput(input)));
            return true;
        }
    }

    public boolean removeSource(Name providingModule, AssetFormat<T> format, Path input) {
        return sources.remove(new Source<>(providingModule, format, new AssetInput(input)));
    }

    public boolean addDeltaSource(Name providingModule, AssetAlterationFormat<T> format, Path input) {
        if (!providingModule.equals(urn.getModuleName()) && !environment.getDependencyNamesOf(providingModule).contains(urn.getModuleName())) {
            logger.warn("Module '{}' provides delta for non-dependency '{}' - {}", providingModule, urn.getModuleName(), urn);
            return false;
        } else {
            deltaSources.add(new Source<>(providingModule, format, new AssetInput(input)));
            return true;
        }
    }

    public boolean removeDeltaSource(Name providingModule, AssetAlterationFormat<T> format, Path input) {
        return deltaSources.remove(new Source<>(providingModule, format, new AssetInput(input)));
    }

    public boolean addSupplementSource(Name providingModule, AssetAlterationFormat<T> format, Path input) {
        if (!providingModule.equals(urn.getModuleName()) && !environment.getDependencyNamesOf(providingModule).contains(urn.getModuleName())) {
            logger.warn("Module '{}' provides supplement for non-dependency '{}' - {}", providingModule, urn.getModuleName(), urn);
            return false;
        } else {
            supplementSources.add(new Source<>(providingModule, format, new AssetInput(input)));
            return true;
        }
    }

    public boolean removeSupplementSource(Name providingModule, AssetAlterationFormat<T> format, Path input) {
        return supplementSources.remove(new Source<>(providingModule, format, new AssetInput(input)));
    }

    public T load() throws IOException {

        AssetSourceResolver assetDataLoader = new AssetSourceResolver();
        T result = assetDataLoader.load();
        if (result != null) {
            Name baseModule = assetDataLoader.getProvidingModule();
            for (Source<AssetAlterationFormat<T>> source : supplementSources) {
                if (source.providingModule.equals(baseModule)) {
                    source.format.apply(source.input, result);
                }
            }
            Collections.sort(deltaSources, new SourceComparator<AssetAlterationFormat<T>>(environment.getModuleIdsOrderedByDependencies()));
            for (Source<AssetAlterationFormat<T>> source : deltaSources) {
                if (source.providingModule.equals(baseModule) || !environment.getDependencyNamesOf(baseModule).contains(source.providingModule)) {
                    source.format.apply(source.input, result);
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return urn.toString();
    }

    private static class Source<U extends Format> {
        private Name providingModule;
        private U format;
        private AssetInput input;

        public Source(Name providingModule, U format, AssetInput input) {
            this.providingModule = providingModule;
            this.format = format;
            this.input = input;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Source) {
                Source other = (Source) obj;
                return Objects.equals(input, other.input);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return input.hashCode();
        }

        @Override
        public String toString() {
            return input.toString();
        }
    }

    private class AssetSourceResolver {
        private Name providingModule;
        private AssetFormat<T> format;
        private List<AssetInput> inputs = Lists.newArrayList();

        public AssetSourceResolver() {
            final List<Name> moduleOrdering = environment.getModuleIdsOrderedByDependencies();
            Collections.sort(sources, new SourceComparator<AssetFormat<T>>(moduleOrdering));
            for (Source<AssetFormat<T>> source : sources) {
                if (providingModule == null) {
                    providingModule = source.providingModule;
                    format = source.format;
                    inputs.add(source.input);
                } else if (providingModule.equals(source.providingModule)) {
                    if (format.equals(source.format)) {
                        inputs.add(source.input);
                    }
                } else if (environment.getDependencyNamesOf(source.providingModule).contains(providingModule)) {
                    providingModule = source.providingModule;
                    format = source.format;
                    inputs.clear();
                    inputs.add(source.input);
                } else if (!environment.getDependencyNamesOf(providingModule).contains(source.providingModule)) {
                    logger.warn("Conflict - both module '{}' and '{}' override {}, selecting '{}'", providingModule, source.providingModule, urn, providingModule);

                }
            }
        }

        public Name getProvidingModule() {
            return providingModule;
        }

        public T load() throws IOException {
            if (providingModule != null) {
                return format.load(urn, inputs);
            }
            return null;
        }
    }

    private static class SourceComparator<T extends Format> implements Comparator<Source<T>> {

        private List<Name> moduleOrdering;

        public SourceComparator(List<Name> moduleOrdering) {
            this.moduleOrdering = moduleOrdering;
        }

        @Override
        public int compare(Source<T> o1, Source<T> o2) {
            return moduleOrdering.indexOf(o1.providingModule) - moduleOrdering.indexOf(o2.providingModule);
        }
    }
}

