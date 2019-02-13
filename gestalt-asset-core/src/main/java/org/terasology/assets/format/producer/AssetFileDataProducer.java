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

package org.terasology.assets.format.producer;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.io.CharStreams;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.assets.format.AssetAlterationFileFormat;
import org.terasology.assets.format.AssetFileFormat;
import org.terasology.assets.format.FileFormat;
import org.terasology.module.resources.FileReference;
import org.terasology.naming.Name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ModuleAssetDataProducer produces asset data from files within modules. In addition to files defining assets, it supports
 * files that override or alter assets defined in other modules, files redirecting a urn to another urn, and the ability
 * to make modifications to asset files in the file system that can be detected and used to reload assets.
 * <p>
 * ModuleAssetDataProducer does not discover files itself. Available files (and changes and removals of available files) should be provided through the {@link FileChangeSubscriber}
 * interface. The available files can also be cleared using {@link #clearAssetFiles()}
 * </p>
 * <p>
 * ModuleAsstDataProducer supports five types of files:
 * </p>
 * <ul>
 * <li>Asset files. These correspond to an AssetFileFormat, and provide the core data for an asset. They are
 * expected to be found under the /assets/<b>folderName</b> directory of modules.</li>
 * <li>Asset Supplementary files. These correspond to an AssetAlterationFileFormat, and provide additional data for an
 * asset. They are expected to be found under the /assets/<b>folderName</b> directory of modules. Supplementary formats
 * can be used by assets of any format - for instance a texture may support both png and jpg formats, and for either a
 * .info file could be provided with additional metadata.</li>
 * <li>Asset redirects. These are used to indicate a urn should be resolved to another urn. These are intended to support
 * assets being renamed or deleted. They are simple text containing the urn to redirect to, with a name corresponding to
 * a urn and a .redirect extension that contain the urn to use instead.
 * Like asset files, they are expected to be found under the /assets/<b>folderName</b> directory of modules.</li>
 * <li>Asset deltas. These are found under /deltas/<b>moduleName</b>/<b>folderName</b>, and provide changes to assets from
 * other modules. An AssetAlterationFileFormat is used to load them.</li>
 * <li>Asset overrides. These are found under /overrides/<b>moduleName</b>/<b>folderName</b>, and replace completely
 * the data of an asset from another module. All the asset formats and asset supplementary formats are used to load these.</li>
 * </ul>
 * <p>
 * When the data for an asset is requested, ModuleAssetDataProducer will return the data using all of the relevant files across
 * all modules.
 * </p>
 *
 * @author Immortius
 */
@ThreadSafe
public class AssetFileDataProducer<U extends AssetData> implements AssetDataProducer<U>, FileChangeSubscriber {

    /**
     * The extension for redirects.
     */
    public static final String REDIRECT_EXTENSION = ".redirect";

    private static final Logger logger = LoggerFactory.getLogger(AssetFileDataProducer.class);

    private final ModuleDependencyProvider dependencyProvider;

    private final ImmutableList<String> folderNames;

    private final List<AssetFileFormat<U>> assetFormats;
    private final List<AssetAlterationFileFormat<U>> deltaFormats;
    private final List<AssetAlterationFileFormat<U>> supplementFormats;

    private final Map<ResourceUrn, UnloadedAssetData<U>> unloadedAssetLookup = new MapMaker().concurrencyLevel(1).makeMap();
    private final Map<ResourceUrn, ResourceUrn> redirectMap = new MapMaker().concurrencyLevel(1).makeMap();
    private final SetMultimap<ResourceUrn, ResourceUrn> redirectSourceMap = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final SetMultimap<Name, Name> resolutionMap = Multimaps.synchronizedSetMultimap(HashMultimap.<Name, Name>create());

    /**
     * Creates a ModuleAssetDataProducer
     *
     * @param dependencyProvider Provider of information on module dependencies
     * @param folderNames        The subfolders that contains files relevant to the asset data this producer loads
     */
    public AssetFileDataProducer(ModuleDependencyProvider dependencyProvider,
                                 String... folderNames) {
        this(dependencyProvider, Arrays.asList(folderNames));
    }

    /**
     * Creates a ModuleAssetDataProducer
     *
     * @param dependencyProvider Provider of information on module dependencies
     * @param folderNames        The subfolders that contains files relevant to the asset data this producer loads
     */
    public AssetFileDataProducer(ModuleDependencyProvider dependencyProvider,
                                 Collection<String> folderNames) {
        this.folderNames = ImmutableList.copyOf(folderNames);
        this.dependencyProvider = dependencyProvider;
        this.assetFormats = Lists.newCopyOnWriteArrayList();
        this.supplementFormats = Lists.newCopyOnWriteArrayList();
        this.deltaFormats = Lists.newCopyOnWriteArrayList();
    }

    /**
     * @return A list of the asset file formats supported
     */
    public List<AssetFileFormat<U>> getAssetFormats() {
        return Collections.unmodifiableList(assetFormats);
    }

    /**
     * @param format The format to add for handling an asset file
     */
    public void addAssetFormat(AssetFileFormat<U> format) {
        assetFormats.add(format);
    }

    /**
     * @param format The format to remove
     * @return Whether the format was successfully removed
     */
    public boolean removeAssetFormat(AssetFileFormat<U> format) {
        return assetFormats.remove(format);
    }

    /**
     * @return A list of the supplement file formats supported
     */
    public List<AssetAlterationFileFormat<U>> getSupplementFormats() {
        return Collections.unmodifiableList(supplementFormats);
    }

    /**
     * @param format A format to add for handling asset supplement files
     */
    public void addSupplementFormat(AssetAlterationFileFormat<U> format) {
        supplementFormats.add(format);
    }

    /**
     * @param format A format to remove
     * @return Whether the format was successfully removed
     */
    public boolean removeSupplementFormat(AssetAlterationFileFormat<U> format) {
        return supplementFormats.remove(format);
    }

    /**
     * @return A list of the delta file formats supported
     */
    public List<AssetAlterationFileFormat<U>> getDeltaFormats() {
        return Collections.unmodifiableList(deltaFormats);
    }

    /**
     * @param format A format to add for handling asset delta files
     */
    public void addDeltaFormat(AssetAlterationFileFormat<U> format) {
        deltaFormats.add(format);
    }

    /**
     * @param format A format to remove
     * @return Whether the format was successfully removed
     */
    public boolean removeDeltaFormat(AssetAlterationFileFormat<U> format) {
        return deltaFormats.remove(format);
    }

    @Override
    public Set<ResourceUrn> getAvailableAssetUrns() {
        return ImmutableSet.copyOf(unloadedAssetLookup.keySet());
    }

    @Override
    public Set<Name> getModulesProviding(Name resourceName) {
        return ImmutableSet.copyOf(resolutionMap.get(resourceName));
    }

    @Override
    public ResourceUrn redirect(ResourceUrn urn) {
        ResourceUrn redirectUrn = redirectMap.get(urn);
        if (redirectUrn != null) {
            return redirectUrn;
        }
        return urn;
    }

    @Override
    public Optional<U> getAssetData(ResourceUrn urn) throws IOException {
        if (urn.getFragmentName().isEmpty()) {
            UnloadedAssetData<U> source = unloadedAssetLookup.get(urn);
            if (source != null && source.isValid()) {
                return source.load();
            }
        }
        return Optional.empty();
    }

    private <V extends FileFormat> Optional<ResourceUrn> registerSource(Name module, FileReference target, Name providingModule,
                                                                        Collection<V> formats, RegisterSourceHandler<U, V> sourceHandler) {
        for (V format : formats) {
            if (format.getFileMatcher().test(target)) {
                try {
                    Name assetName = format.getAssetName(target.getName());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                    if (existing != null) {
                        if (sourceHandler.registerSource(existing, providingModule, format, target)) {
                            return Optional.of(urn);
                        }
                    } else {
                        UnloadedAssetData<U> source = new UnloadedAssetData<>(urn, dependencyProvider);
                        if (sourceHandler.registerSource(source, providingModule, format, target)) {
                            unloadedAssetLookup.put(urn, source);
                            resolutionMap.put(urn.getResourceName(), urn.getModuleName());
                            return Optional.of(urn);
                        }
                    }
                    return Optional.empty();
                } catch (InvalidAssetFilenameException e) {
                    logger.warn("Invalid name for asset - {}", target);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<ResourceUrn> registerAssetDelta(Name module, FileReference target, Name providingModule) {
        for (AssetAlterationFileFormat<U> format : deltaFormats) {
            if (format.getFileMatcher().test(target)) {
                try {
                    Name assetName = format.getAssetName(target.getName());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> unloadedAssetData = unloadedAssetLookup.get(urn);
                    if (unloadedAssetData == null) {
                        logger.warn("Discovered delta for unknown asset '{}'", urn);
                        return Optional.empty();
                    }
                    if (unloadedAssetData.addDeltaSource(providingModule, format, target)) {
                        return Optional.of(urn);
                    }
                } catch (InvalidAssetFilenameException e) {
                    logger.error("Invalid file name '{}' for asset delta", target, e);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> assetFileAdded(FileReference file, Name module, Name providingModule) {
        if (file.getName().endsWith(REDIRECT_EXTENSION)) {
            processRedirectFile(file, module);
        } else {
            Optional<ResourceUrn> urn = registerSource(module, file, providingModule, assetFormats, UnloadedAssetData::addSource);
            if (!urn.isPresent()) {
                urn = registerSource(module, file, providingModule, supplementFormats, UnloadedAssetData::addSupplementSource);
            }
            if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
                return urn;
            }
        }
        return Optional.empty();
    }

    private synchronized void processRedirectFile(FileReference file, Name moduleId) {
        Name assetName = new Name(com.google.common.io.Files.getNameWithoutExtension(file.getName()));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.open(), Charsets.UTF_8))) {
            List<String> contents = CharStreams.readLines(reader);
            if (contents.isEmpty()) {
                logger.error("Failed to read redirect '{}:{}' - empty", moduleId, assetName);
            } else if (!ResourceUrn.isValid(contents.get(0))) {
                logger.error("Failed to read redirect '{}:{}' - '{}' is not a valid urn", moduleId, assetName, contents.get(0));
            } else {

                ResourceUrn fromUrn = new ResourceUrn(moduleId, assetName);
                ResourceUrn toUrn = new ResourceUrn(contents.get(0));
                if (redirectMap.containsKey(toUrn)) {
                    toUrn = redirectMap.get(toUrn);
                }

                redirectMap.put(fromUrn, toUrn);
                redirectSourceMap.put(toUrn, fromUrn);
                for (ResourceUrn furtherFromUrn : redirectSourceMap.get(fromUrn)) {
                    redirectMap.put(furtherFromUrn, toUrn);
                    redirectSourceMap.put(toUrn, furtherFromUrn);
                }
                redirectSourceMap.removeAll(fromUrn);

                resolutionMap.put(assetName, moduleId);
            }
        } catch (IOException e) {
            logger.error("Failed to read redirect '{}:{}'", moduleId, assetName, e);
        }
    }

    private Optional<ResourceUrn> getResourceUrn(FileReference target, Name module, Collection<? extends FileFormat> formats) {
        for (FileFormat fileFormat : formats) {
            if (fileFormat.getFileMatcher().test(target)) {
                try {
                    Name assetName = fileFormat.getAssetName(target.getName());
                    return Optional.of(new ResourceUrn(module, assetName));
                } catch (InvalidAssetFilenameException e) {
                    logger.debug("Modified file does not have a valid asset name - '{}'", target);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> assetFileModified(FileReference file, Name module, Name providingModule) {
        Optional<ResourceUrn> urn = getResourceUrn(file, module, assetFormats);
        if (!urn.isPresent()) {
            urn = getResourceUrn(file, module, supplementFormats);
        }
        if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
            return urn;
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> assetFileDeleted(FileReference file, Name module, Name providingModule) {
        for (AssetFileFormat<U> format : assetFormats) {
            if (format.getFileMatcher().test(file)) {
                try {
                    Name assetName = format.getAssetName(file.getName());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                    if (existing != null) {
                        existing.removeSource(providingModule, format, file);
                        if (existing.isValid()) {
                            return Optional.of(urn);
                        }
                    }
                    return Optional.empty();
                } catch (InvalidAssetFilenameException e) {
                    logger.debug("Deleted file does not have a valid file name - {}", file);
                }
            }
        }
        for (AssetAlterationFileFormat<U> format : supplementFormats) {
            if (format.getFileMatcher().test(file)) {
                try {
                    Name assetName = format.getAssetName(file.getName());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                    if (existing != null) {
                        existing.removeSupplementSource(providingModule, format, file);
                        if (existing.isValid()) {
                            return Optional.of(urn);
                        }
                    }
                    return Optional.empty();
                } catch (InvalidAssetFilenameException e) {
                    logger.debug("Deleted file does not have a valid file name - {}", file);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> deltaFileAdded(FileReference file, Name module, Name providingModule) {
        Optional<ResourceUrn> urn = registerAssetDelta(module, file, providingModule);
        if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
            return urn;
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> deltaFileModified(FileReference file, Name module, Name providingModule) {
        Optional<ResourceUrn> urn = getResourceUrn(file, module, deltaFormats);
        if (urn.isPresent()) {
            if (unloadedAssetLookup.get(urn.get()).isValid()) {
                return urn;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> deltaFileDeleted(FileReference file, Name module, Name providingModule) {
        for (AssetAlterationFileFormat<U> format : deltaFormats) {
            if (format.getFileMatcher().test(file)) {
                try {
                    Name assetName = format.getAssetName(file.getName());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                    if (existing != null) {
                        existing.removeDeltaSource(providingModule, format, file);
                        if (existing.isValid()) {
                            return Optional.of(urn);
                        }
                    }
                    return Optional.empty();
                } catch (InvalidAssetFilenameException e) {
                    logger.debug("Deleted file does not have a valid file name - {}", file);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Remove all asset files.
     */
    public void clearAssetFiles() {
        unloadedAssetLookup.clear();
        redirectMap.clear();
        redirectSourceMap.clear();
        resolutionMap.clear();
    }

    /**
     * @return The list of asset folders used by this AssetFileDataProducer
     */
    public List<String> getFolderNames() {
        return folderNames;
    }

    /**
     * Interface for registering a source. Allows the same outer logic to be used for registering different types of asset sources.
     *
     * @param <T>
     * @param <U>
     */
    private interface RegisterSourceHandler<T extends AssetData, U extends FileFormat> {
        boolean registerSource(UnloadedAssetData<T> source, Name providingModule, U format, FileReference input);
    }

}
