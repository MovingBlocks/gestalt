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

package org.terasology.assets.module;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.assets.format.AssetAlterationFileFormat;
import org.terasology.assets.format.AssetFileFormat;
import org.terasology.assets.format.FileFormat;
import org.terasology.assets.module.autoreload.AssetFileChangeSubscriber;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ModuleAssetDataProducer produces asset data from files within modules. In addition to files defining assets, it supports
 * files that override or alter assets defined in other modules, files redirecting a urn to another urn, and the ability
 * to make modifications to asset files in the file system that can be detected and used to reload assets.
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
 * <p>
 * ModuleAssetDataProducer also sets up watchers for any modules that are folders on the file system. This allows the file
 * system to be checked for any changed assets, and these assets reloaded as desired.
 * </p>
 *
 * @author Immortius
 */
@ThreadSafe
public class ModuleAssetDataProducer<U extends AssetData> implements AssetDataProducer<U>, AssetFileChangeSubscriber {

    /**
     * The name of the module directory that contains asset files.
     */
    public static final String ASSET_FOLDER = "assets";

    /**
     * The name of the module directory that contains overrides.
     */
    public static final String OVERRIDE_FOLDER = "overrides";

    /**
     * The name of the module directory that contains detlas.
     */
    public static final String DELTA_FOLDER = "deltas";

    /**
     * The extension for redirects.
     */
    public static final String REDIRECT_EXTENSION = ".redirect";

    private static final Logger logger = LoggerFactory.getLogger(ModuleAssetDataProducer.class);

    private final ImmutableList<String> folderNames;

    private final ModuleEnvironment moduleEnvironment;

    private final ImmutableList<AssetFileFormat<U>> assetFormats;
    private final ImmutableList<AssetAlterationFileFormat<U>> deltaFormats;
    private final ImmutableList<AssetAlterationFileFormat<U>> supplementFormats;

    private final Map<ResourceUrn, UnloadedAssetData<U>> unloadedAssetLookup = new MapMaker().concurrencyLevel(1).makeMap();
    private final Map<ResourceUrn, ResourceUrn> redirectMap = new MapMaker().concurrencyLevel(1).makeMap();
    private final SetMultimap<ResourceUrn, ResourceUrn> redirectSourceMap = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final SetMultimap<Name, Name> resolutionMap = Multimaps.synchronizedSetMultimap(HashMultimap.<Name, Name>create());

    /**
     * Creates a ModuleAssetDataProducer
     *
     * @param moduleEnvironment   The module environment to load asset data from
     * @param assetFormats        The file formats supported for loading asset files
     * @param supplementalFormats The supplementary file formats supported when loading asset files
     * @param deltaFormats        The delta file formats supported when loading asset files
     * @param folderNames         The subfolders that contains files relevant to the asset data this producer loads
     */
    public ModuleAssetDataProducer(ModuleEnvironment moduleEnvironment,
                                   Collection<AssetFileFormat<U>> assetFormats,
                                   Collection<AssetAlterationFileFormat<U>> supplementalFormats,
                                   Collection<AssetAlterationFileFormat<U>> deltaFormats,
                                   String... folderNames) {
        this(moduleEnvironment, assetFormats, supplementalFormats, deltaFormats, Arrays.asList(folderNames));
    }

    /**
     * Creates a ModuleAssetDataProducer
     *
     * @param moduleEnvironment   The module environment to load asset data from
     * @param assetFormats        The file formats supported for loading asset files
     * @param supplementalFormats The supplementary file formats supported when loading asset files
     * @param deltaFormats        The delta file formats supported when loading asset files
     * @param folderNames         The subfolders that contains files relevant to the asset data this producer loads
     */
    public ModuleAssetDataProducer(ModuleEnvironment moduleEnvironment,
                                   Collection<AssetFileFormat<U>> assetFormats,
                                   Collection<AssetAlterationFileFormat<U>> supplementalFormats,
                                   Collection<AssetAlterationFileFormat<U>> deltaFormats,
                                   Collection<String> folderNames) {
        this.folderNames = ImmutableList.copyOf(folderNames);
        this.moduleEnvironment = moduleEnvironment;
        this.assetFormats = ImmutableList.copyOf(assetFormats);
        this.supplementFormats = ImmutableList.copyOf(supplementalFormats);
        this.deltaFormats = ImmutableList.copyOf(deltaFormats);
    }

    /**
     * @return A list of the asset file formats supported
     */
    public ImmutableList<AssetFileFormat<U>> getAssetFormats() {
        return assetFormats;
    }

    /**
     * @return A list of the supplement file formats supported
     */
    public ImmutableList<AssetAlterationFileFormat<U>> getSupplementFormats() {
        return supplementFormats;
    }

    /**
     * @return A list of the delta file formats supported
     */
    public ImmutableList<AssetAlterationFileFormat<U>> getDeltaFormats() {
        return deltaFormats;
    }

    /**
     * @return The module environment that asset data is read from
     */
    public ModuleEnvironment getModuleEnvironment() {
        return moduleEnvironment;
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

    private <V extends FileFormat> Optional<ResourceUrn> registerSource(Name module, Path target, Name providingModule,
                                                                        Collection<V> formats, RegisterSourceHandler<U, V> sourceHandler) {
        Path filename = target.getFileName();
        if (filename == null) {
            logger.error("Missing filename for asset file");
            return Optional.empty();
        }
        for (V format : formats) {
            if (format.getFileMatcher().matches(target)) {
                try {
                    Name assetName = format.getAssetName(filename.toString());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                    if (existing != null) {
                        if (sourceHandler.registerSource(existing, providingModule, format, target)) {
                            return Optional.of(urn);
                        }
                    } else {
                        UnloadedAssetData<U> source = new UnloadedAssetData<>(urn, moduleEnvironment);
                        if (sourceHandler.registerSource(source, providingModule, format, target)) {
                            unloadedAssetLookup.put(urn, source);
                            resolutionMap.put(urn.getResourceName(), urn.getModuleName());
                            return Optional.of(urn);
                        }
                    }
                    return Optional.empty();
                } catch (InvalidAssetFilenameException e) {
                    logger.warn("Invalid name for asset - {}", filename);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<ResourceUrn> registerAssetDelta(Name module, Path target, Name providingModule) {
        Path filename = target.getFileName();
        if (filename == null) {
            logger.error("Missing file name for asset delta for '{}'", target);
            return Optional.empty();
        }
        for (AssetAlterationFileFormat<U> format : deltaFormats) {
            if (format.getFileMatcher().matches(target)) {
                try {
                    Name assetName = format.getAssetName(filename.toString());
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
                    logger.error("Invalid file name '{}' for asset delta", target.getFileName(), e);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> assetFileAdded(Path path, Name module, Name providingModule) {
        if (path.getFileName().toString().endsWith(REDIRECT_EXTENSION)) {
            processRedirectFile(path, module);
        } else {
            Optional<ResourceUrn> urn = registerSource(module, path, providingModule, assetFormats, UnloadedAssetData::addSource);
            if (!urn.isPresent()) {
                urn = registerSource(module, path, providingModule, supplementFormats, UnloadedAssetData::addSupplementSource);
            }
            if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
                return urn;
            }
        }
        return Optional.empty();
    }

    private synchronized void processRedirectFile(Path file, Name moduleId) {
        Path filename = file.getFileName();
        if (filename != null) {
            Name assetName = new Name(com.google.common.io.Files.getNameWithoutExtension(filename.toString()));
            try (BufferedReader reader = Files.newBufferedReader(file, Charsets.UTF_8)) {
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
        } else {
            logger.error("Missing file name for redirect");
        }
    }

    private Optional<ResourceUrn> getResourceUrn(Path target, Name module, Collection<? extends FileFormat> formats) {
        Path filename = target.getFileName();
        if (filename != null) {
            for (FileFormat fileFormat : formats) {
                if (fileFormat.getFileMatcher().matches(target)) {
                    try {
                        Name assetName = fileFormat.getAssetName(filename.toString());
                        return Optional.of(new ResourceUrn(module, assetName));
                    } catch (InvalidAssetFilenameException e) {
                        logger.debug("Modified file does not have a valid asset name - '{}'", filename);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> assetFileModified(Path path, Name module, Name providingModule) {
        Optional<ResourceUrn> urn = getResourceUrn(path, module, assetFormats);
        if (!urn.isPresent()) {
            urn = getResourceUrn(path, module, supplementFormats);
        }
        if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
            return urn;
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> assetFileDeleted(Path path, Name module, Name providingModule) {
        Path filename = path.getFileName();
        if (filename != null) {
            for (AssetFileFormat<U> format : assetFormats) {
                if (format.getFileMatcher().matches(path)) {
                    try {
                        Name assetName = format.getAssetName(filename.toString());
                        ResourceUrn urn = new ResourceUrn(module, assetName);
                        UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                        if (existing != null) {
                            existing.removeSource(providingModule, format, path);
                            if (existing.isValid()) {
                                return Optional.of(urn);
                            }
                        }
                        return Optional.empty();
                    } catch (InvalidAssetFilenameException e) {
                        logger.debug("Deleted file does not have a valid file name - {}", path);
                    }
                }
            }
            for (AssetAlterationFileFormat<U> format : supplementFormats) {
                if (format.getFileMatcher().matches(path)) {
                    try {
                        Name assetName = format.getAssetName(filename.toString());
                        ResourceUrn urn = new ResourceUrn(module, assetName);
                        UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                        if (existing != null) {
                            existing.removeSupplementSource(providingModule, format, path);
                            if (existing.isValid()) {
                                return Optional.of(urn);
                            }
                        }
                        return Optional.empty();
                    } catch (InvalidAssetFilenameException e) {
                        logger.debug("Deleted file does not have a valid file name - {}", path);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> deltaFileAdded(Path path, Name module, Name providingModule) {
        Optional<ResourceUrn> urn = registerAssetDelta(module, path, providingModule);
        if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
            return urn;
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> deltaFileModified(Path path, Name module, Name providingModule) {
        Optional<ResourceUrn> urn = getResourceUrn(path, module, deltaFormats);
        if (urn.isPresent()) {
            if (unloadedAssetLookup.get(urn.get()).isValid()) {
                return urn;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceUrn> deltaFileDeleted(Path path, Name module, Name providingModule) {
        Path filename = path.getFileName();
        if (filename == null) {
            logger.error("Missing filename for deleted file");
            return Optional.empty();
        }
        for (AssetAlterationFileFormat<U> format : deltaFormats) {
            if (format.getFileMatcher().matches(path)) {
                try {
                    Name assetName = format.getAssetName(filename.toString());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                    if (existing != null) {
                        existing.removeDeltaSource(providingModule, format, path);
                        if (existing.isValid()) {
                            return Optional.of(urn);
                        }
                    }
                    return Optional.empty();
                } catch (InvalidAssetFilenameException e) {
                    logger.debug("Deleted file does not have a valid file name - {}", path);
                }
            }
        }
        return Optional.empty();
    }

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
        boolean registerSource(UnloadedAssetData<T> source, Name providingModule, U format, Path input);
    }

}
