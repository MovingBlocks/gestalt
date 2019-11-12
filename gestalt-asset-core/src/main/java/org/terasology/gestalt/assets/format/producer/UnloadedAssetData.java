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

package org.terasology.gestalt.assets.format.producer;

import com.google.common.collect.Lists;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.assets.AssetData;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.format.AssetAlterationFileFormat;
import org.terasology.gestalt.assets.format.AssetDataFile;
import org.terasology.gestalt.assets.format.AssetFileFormat;
import org.terasology.gestalt.assets.format.FileFormat;
import org.terasology.gestalt.module.resources.FileReference;
import org.terasology.gestalt.naming.Name;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the details of an available but unloaded asset data. This includes all primary sources, supplements, deltas and overrides.
 * <p>The sources to use are determined when the load is requested - this allows for the sources to be added to or removed due to changes on the file system</p>
 *
 * @author Immortius
 */
@ThreadSafe
class UnloadedAssetData<T extends AssetData> {
    private static final Logger logger = LoggerFactory.getLogger(UnloadedAssetData.class);

    private final ModuleDependencyProvider moduleDependencies;
    private final ResourceUrn urn;
    private final List<Source<AssetFileFormat<T>>> sources = Collections.synchronizedList(Lists.newArrayList());
    private final List<Source<AssetAlterationFileFormat<T>>> supplementSources = Collections.synchronizedList(Lists.newArrayList());
    private final List<Source<AssetAlterationFileFormat<T>>> deltaSources = Collections.synchronizedList(Lists.newArrayList());

    /**
     * @param urn                The urn of the asset this unloaded asset data corresponds to.
     * @param moduleDependencies A provider of information on how modules depend on each other.
     */
    public UnloadedAssetData(ResourceUrn urn, ModuleDependencyProvider moduleDependencies) {
        this.urn = urn;
        this.moduleDependencies = moduleDependencies;
    }

    /**
     * @return The urn of the resource this unloaded asset data belongs to
     */
    public ResourceUrn getUrn() {
        return urn;
    }

    /**
     * An UnloadedAssetData is not valid if it has no non-supplemental and non-delta sources.
     *
     * @return Whether this UnloadedAssetData has the minimum required sources to be loaded
     */
    public boolean isValid() {
        return !sources.isEmpty();
    }

    /**
     * Adds a primary source or override source. The providing module must be either be the module the asset belongs to, or one that has that module as a dependency.
     *
     * @param providingModule The module providing the source
     * @param format          The format the source can be loaded with
     * @param input           The path of the source
     * @return Whether the source was added successfully.
     */
    public boolean addSource(Name providingModule, AssetFileFormat<T> format, FileReference input) {
        if (!providingModule.equals(urn.getModuleName()) && !moduleDependencies.dependencyExists(providingModule, urn.getModuleName())) {
            logger.warn("Module '{}' provides override for non-dependency '{}' - {}", providingModule, urn.getModuleName(), urn);
            return false;
        } else {
            sources.add(new Source<>(providingModule, format, new AssetDataFile(input)));
            return true;
        }
    }

    /**
     * Removes a primary source.
     *
     * @param providingModule The module providing the source
     * @param format          The format the source would be loaded with
     * @param input           The path of the source
     * @return Whether the source was removed - it will not be if it wasn't previously successfully removed.
     */
    public boolean removeSource(Name providingModule, AssetFileFormat<T> format, FileReference input) {
        return sources.remove(new Source<>(providingModule, format, new AssetDataFile(input)));
    }

    /**
     * Adds a delta source. The providing module must either be the module the asset belongs to, or one that has that module as a dependency.
     *
     * @param providingModule The module providing the source
     * @param format          The format the source can be loaded with
     * @param input           The path of the source
     * @return Whether the source was added successfully.
     */
    public boolean addDeltaSource(Name providingModule, AssetAlterationFileFormat<T> format, FileReference input) {
        if (!providingModule.equals(urn.getModuleName()) && !moduleDependencies.dependencyExists(providingModule, urn.getModuleName())) {
            logger.warn("Module '{}' provides delta for non-dependency '{}' - {}", providingModule, urn.getModuleName(), urn);
            return false;
        } else {
            deltaSources.add(new Source<>(providingModule, format, new AssetDataFile(input)));
            return true;
        }
    }

    /**
     * Removes a delta source
     *
     * @param providingModule The module providing the source
     * @param format          The format the source can be loaded with
     * @param input           The path of the source
     * @return Whether the source was removed - it will not be if the source was not added previously
     */
    public boolean removeDeltaSource(Name providingModule, AssetAlterationFileFormat<T> format, FileReference input) {
        return deltaSources.remove(new Source<>(providingModule, format, new AssetDataFile(input)));
    }

    /**
     * Adds a supplemental source. The providing module must either be the module the asset belongs to, or one that has that module as a dependency.
     *
     * @param providingModule The module providing the source
     * @param format          The format the source can be loaded with
     * @param input           The path of the source
     * @return Whether the source was added successfully.
     */
    public boolean addSupplementSource(Name providingModule, AssetAlterationFileFormat<T> format, FileReference input) {
        if (!providingModule.equals(urn.getModuleName()) && !moduleDependencies.dependencyExists(providingModule, urn.getModuleName())) {
            logger.warn("Module '{}' provides supplement for non-dependency '{}' - {}", providingModule, urn.getModuleName(), urn);
            return false;
        } else {
            supplementSources.add(new Source<>(providingModule, format, new AssetDataFile(input)));
            return true;
        }
    }

    /**
     * Removes a supplemental source
     *
     * @param providingModule The module providing the source
     * @param format          The format the source can be loaded with
     * @param input           The path of the source
     * @return Whether the source was removed - it will not be if the source was not added previously
     */
    public boolean removeSupplementSource(Name providingModule, AssetAlterationFileFormat<T> format, FileReference input) {
        return supplementSources.remove(new Source<>(providingModule, format, new AssetDataFile(input)));
    }

    /**
     * Loads the asset data from the sources. This determines which primary and supplemental sources to use (taking into account overrides), which deltas to apply
     * and in which order (again taking into account overrides, as well as the module dependency hierarchy).
     *
     * @return An Optional with the asset data if loaded, or absent if the {@link #isValid} is false
     * @throws IOException If there as an issue loading the asset data.
     */
    public Optional<T> load() throws IOException {
        AssetSourceResolver assetDataLoader = new AssetSourceResolver();
        T result = assetDataLoader.load();
        if (result != null) {
            Name baseModule = assetDataLoader.getProvidingModule();
            synchronized (supplementSources) {
                for (Source<AssetAlterationFileFormat<T>> source : supplementSources) {
                    if (source.providingModule.equals(baseModule)) {
                        source.format.apply(source.input, result);
                    }
                }
            }
            synchronized (deltaSources) {
                deltaSources.sort(new SourceComparator<>(moduleDependencies.getModulesOrderedByDependency()));
                for (Source<AssetAlterationFileFormat<T>> source : deltaSources) {
                    if (source.providingModule.equals(baseModule) || !moduleDependencies.dependencyExists(baseModule, source.providingModule)) {
                        source.format.apply(source.input, result);
                    }
                }
            }
            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return urn.toString();
    }

    /**
     * A source encapsulates a providing module, file format and input path.
     *
     * @param <U> The type of the file format
     */
    private static class Source<U extends FileFormat> {
        private final Name providingModule;
        private final U format;
        private final AssetDataFile input;

        public Source(Name providingModule, U format, AssetDataFile input) {
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

    /**
     * A comparator for ordering sources by their providing module.
     *
     * @param <T> The type of the file format of the sources being ordered
     */
    private static class SourceComparator<T extends FileFormat> implements Comparator<Source<T>> {

        private final List<Name> moduleOrdering;

        public SourceComparator(List<Name> moduleOrdering) {
            this.moduleOrdering = moduleOrdering;
        }

        @Override
        public int compare(Source<T> o1, Source<T> o2) {
            return moduleOrdering.indexOf(o1.providingModule) - moduleOrdering.indexOf(o2.providingModule);
        }
    }

    /**
     * Determines the module and primary source that will provide the asset data.
     * <p>
     * The sources are first ordered by the module dependency order - this ensures consistent ordering. The first source determines the initial providing module. As the
     * sources are iterated through, if the module providing the source depends on the current providing module it becomes the new providing module.
     * </p>
     */
    private class AssetSourceResolver {
        private Name providingModule;
        private AssetFileFormat<T> format;
        private List<AssetDataFile> inputs = Lists.newArrayList();

        public AssetSourceResolver() {
            final List<Name> moduleOrdering = moduleDependencies.getModulesOrderedByDependency();
            synchronized (sources) {
                sources.sort(new SourceComparator<>(moduleOrdering));
                for (Source<AssetFileFormat<T>> source : sources) {
                    if (providingModule == null) {
                        providingModule = source.providingModule;
                        format = source.format;
                        inputs.add(source.input);
                    } else if (providingModule.equals(source.providingModule)) {
                        if (format.equals(source.format)) {
                            inputs.add(source.input);
                        }
                    } else if (moduleDependencies.dependencyExists(source.providingModule, providingModule)) {
                        providingModule = source.providingModule;
                        format = source.format;
                        inputs.clear();
                        inputs.add(source.input);
                    } else if (!moduleDependencies.dependencyExists(providingModule, source.providingModule)) {
                        logger.warn("Conflict - both module '{}' and '{}' override {}, selecting '{}'", providingModule, source.providingModule, urn, providingModule);
                    }
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
}

