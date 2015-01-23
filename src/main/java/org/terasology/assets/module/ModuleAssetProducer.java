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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetProducer;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.filesystem.ModuleFileSystemProvider;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;
import org.terasology.util.io.FileScanning;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Immortius
 */
public class ModuleAssetProducer<U extends AssetData> implements AssetProducer<U> {

    public static final String ASSET_FOLDER = "assets";
    public static final String OVERRIDE_FOLDER = "overrides";
    public static final String DELTA_FOLDER = "deltas";

    private static final Logger logger = LoggerFactory.getLogger(ModuleAssetProducer.class);

    private final String assetId;
    private final String folderName;

    private ModuleEnvironment moduleEnvironment;

    private List<AssetFormat<U>> formats = Lists.newArrayList();
    private List<AssetDeltaFormat<U>> deltaFormats = Lists.newArrayList();
    private Table<Name, Name, UnloadedAsset<U>> unloadedAssetLookup = HashBasedTable.create();
    private ListMultimap<ResourceUrn, UnloadedAssetDelta<U>> assetDeltaLookup = ArrayListMultimap.create();

    public ModuleAssetProducer(String assetId, String folderName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(assetId), "assetId must not be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(folderName), "folderName must not be null or empty");
        this.assetId = assetId;
        this.folderName = folderName;
    }

    public void setEnvironment(ModuleEnvironment environment) {
        Preconditions.checkNotNull(environment);
        this.moduleEnvironment = environment;

        unloadedAssetLookup.clear();
        assetDeltaLookup.clear();
        scanForAssets();
        Map<ResourceUrn, Name> overriddenByModule = scanForOverrides();
        scanForDeltas(overriddenByModule);
    }

    @Override
    public Set<ResourceUrn> resolve(String urn, Name moduleContext) {
        final Name resourceName = new Name(urn);
        if (moduleContext != null && !moduleContext.isEmpty()) {
            if (unloadedAssetLookup.contains(resourceName, moduleContext)) {
                return ImmutableSet.of(new ResourceUrn(moduleContext, resourceName));
            }
            Set<ResourceUrn> resources = Sets.newLinkedHashSet();
            for (Name dependency : moduleEnvironment.getDependencyNamesOf(moduleContext)) {
                if (unloadedAssetLookup.contains(resourceName, dependency)) {
                    resources.add(new ResourceUrn(dependency, resourceName));
                }
            }
            if (!resources.isEmpty()) {
                return resources;
            }
        }

        Map<Name, UnloadedAsset<U>> availableResources = unloadedAssetLookup.row(resourceName);
        return Sets.newLinkedHashSet(Collections2.transform(availableResources.keySet(), new Function<Name, ResourceUrn>() {
            @Nullable
            @Override
            public ResourceUrn apply(Name moduleName) {
                return new ResourceUrn(moduleName, resourceName);
            }
        }));
    }

    @Override
    public U getAssetData(ResourceUrn urn) throws IOException {
        if (urn.getFragmentName().isEmpty()) {
            UnloadedAsset<U> source = unloadedAssetLookup.get(urn.getResourceName(), urn.getModuleName());
            if (source != null) {
                return source.load();
            }
        }
        return null;
    }

    public List<AssetFormat<U>> getFormats() {
        return Collections.unmodifiableList(formats);
    }

    public void addFormat(AssetFormat<U> format) {
        formats.add(format);
    }

    public List<AssetDeltaFormat<U>> getDeltaFormats() {
        return Collections.unmodifiableList(deltaFormats);
    }

    public void addDeltaFormat(AssetDeltaFormat<U> format) {
        deltaFormats.add(format);
    }

    private void scanForAssets() {
        for (AssetFormat<U> format : formats) {
            for (Module module : moduleEnvironment) {
                Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, ASSET_FOLDER, folderName);
                if (Files.exists(rootPath)) {
                    Map<Name, UnloadedAsset<U>> moduleSources = Maps.newLinkedHashMap();
                    try {
                        for (Path file : module.findFiles(rootPath, FileScanning.acceptAll(), format.getFileMatcher())) {
                            try {
                                Name assetName = format.getAssetName(file.getFileName().toString());
                                UnloadedAsset<U> source = unloadedAssetLookup.get(assetName, module.getId());
                                if (source == null) {
                                    source = new UnloadedAsset<>(new ResourceUrn(module.getId(), assetName), format);
                                    moduleSources.put(assetName, source);
                                }
                                source.addInput(file);
                            } catch (InvalidAssetFilenameException e) {
                                logger.error("Invalid file name '{}' for asset type '{}", file.getFileName(), assetId, e);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Failed to scan module '{}' for assets", module.getId(), e);
                    }
                    unloadedAssetLookup.column(module.getId()).putAll(moduleSources);
                }
            }
        }
    }

    private Map<ResourceUrn, Name> scanForOverrides() {
        Map<ResourceUrn, Name> overriddenByModule = Maps.newHashMap();
        for (Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, OVERRIDE_FOLDER);
            if (Files.exists(rootPath)) {
                Map<ResourceUrn, UnloadedAsset<U>> moduleOverrides = Maps.newLinkedHashMap();
                for (AssetFormat<U> format : formats) {
                    moduleOverrides.putAll(scanForOverridesOfFormat(module, rootPath, format));
                }
                Set<Name> moduleDependencies = moduleEnvironment.getDependencyNamesOf(module.getId());
                for (Map.Entry<ResourceUrn, UnloadedAsset<U>> entry : moduleOverrides.entrySet()) {
                    Name oldModule = overriddenByModule.put(entry.getKey(), module.getId());
                    if (oldModule != null && !moduleDependencies.contains(oldModule)) {
                        logger.warn("Conflicting overrides of '{}', applying from '{}' over '{}'", entry.getKey(), module.getId(), oldModule);
                    }
                    unloadedAssetLookup.put(entry.getKey().getResourceName(), entry.getKey().getModuleName(), entry.getValue());
                }
            }
        }
        return overriddenByModule;
    }

    private void scanForDeltas(Map<ResourceUrn, Name> overriddenByModule) {
        for (Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, DELTA_FOLDER);
            if (Files.exists(rootPath)) {
                for (AssetDeltaFormat<U> format : deltaFormats) {
                    for (Map.Entry<ResourceUrn, UnloadedAssetDelta<U>> delta : scanForDeltasOfFormat(module, rootPath, format).entrySet()) {
                        Name overrideModule = overriddenByModule.get(delta.getKey());
                        if (overrideModule != null && moduleEnvironment.getDependencyNamesOf(overrideModule).contains(delta.getKey().getModuleName())) {
                            continue;
                        }

                        UnloadedAsset<U> asset = unloadedAssetLookup.get(delta.getKey().getResourceName(), delta.getKey().getModuleName());
                        if (asset != null) {
                            asset.addDelta(delta.getValue());
                        } else {
                            logger.warn("Discovered delta for unknown asset '{}'", delta.getKey());
                        }
                    }
                }
            }
        }
    }

    private Map<ResourceUrn, UnloadedAssetDelta<U>> scanForDeltasOfFormat(Module origin, Path rootPath, AssetDeltaFormat<U> format) {
        Map<ResourceUrn, UnloadedAssetDelta<U>> discoveredDeltas = Maps.newLinkedHashMap();
        try {
            for (Path file : origin.findFiles(rootPath, FileScanning.acceptAll(), new FormatMatcher(folderName, format.getFileMatcher()))) {
                try {
                    Name assetName = format.getAssetName(file.getFileName().toString());
                    Name moduleName = new Name(file.getName(1).toString());
                    if (!moduleEnvironment.getDependencyNamesOf(origin.getId()).contains(moduleName)) {
                        logger.warn("Module '{}' contains delta for non-dependency '{}', skipping", origin, moduleName);
                        continue;
                    }
                    ResourceUrn urn = new ResourceUrn(moduleName, assetName);
                    UnloadedAssetDelta<U> source = discoveredDeltas.get(urn);
                    if (source == null) {
                        source = new UnloadedAssetDelta<>(format);
                        discoveredDeltas.put(urn, source);
                    }
                    source.addInput(file);
                } catch (InvalidAssetFilenameException e) {
                    logger.error("Invalid file name '{}' for asset delta for type '{}", file.getFileName(), assetId, e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan for deltas of '" + assetId + "'", e);
        }
        return discoveredDeltas;
    }

    private Map<ResourceUrn, UnloadedAsset<U>> scanForOverridesOfFormat(Module origin, Path rootPath, AssetFormat<U> format) {
        Map<ResourceUrn, UnloadedAsset<U>> newOverrides = Maps.newLinkedHashMap();
        try {
            for (Path file : origin.findFiles(rootPath, FileScanning.acceptAll(), new FormatMatcher(folderName, format.getFileMatcher()))) {
                try {
                    Name assetName = format.getAssetName(file.getFileName().toString());
                    Name moduleName = new Name(file.getName(1).toString());
                    if (!moduleEnvironment.getDependencyNamesOf(origin.getId()).contains(moduleName)) {
                        logger.warn("Module '{}' contains overrides for non-dependency '{}', skipping", origin, moduleName);
                        continue;
                    }
                    ResourceUrn urn = new ResourceUrn(moduleName, assetName);
                    UnloadedAsset<U> source = newOverrides.get(urn);
                    if (source == null) {
                        source = new UnloadedAsset<>(urn, format);
                        newOverrides.put(urn, source);
                    }
                    source.addInput(file);
                } catch (InvalidAssetFilenameException e) {
                    logger.error("Invalid file name '{}' for asset type '{}", file.getFileName(), assetId, e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan for overrides of '" + assetId + "'", e);
        }
        return newOverrides;
    }

    public void removeFormat(AssetFormat<U> format) {
        formats.remove(format);
    }

    public void removeAllFormats() {
        formats.clear();
    }

    private static class FormatMatcher implements PathMatcher {

        private final String folderName;
        private final PathMatcher formatMatcher;

        public FormatMatcher(String folderName, PathMatcher formatMatcher) {
            this.folderName = folderName;
            this.formatMatcher = formatMatcher;
        }

        @Override
        public boolean matches(Path path) {
            return path.getNameCount() > 2 && path.getName(2).toString().equalsIgnoreCase(folderName) && formatMatcher.matches(path);
        }
    }
}
