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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetProducer;
import org.terasology.assets.exceptions.InvalidAssetFilenameException;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.filesystem.ModuleFileSystemProvider;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;
import org.terasology.util.io.FileExtensionPathMatcher;
import org.terasology.util.io.FileScanning;

import javax.annotation.Nullable;
import java.io.BufferedReader;
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
    public static final String REDIRECT_EXTENSION = "redirect";

    private static final Logger logger = LoggerFactory.getLogger(ModuleAssetProducer.class);

    private final Class<U> assetDataClass;
    private final String folderName;

    private ModuleEnvironment moduleEnvironment;

    private List<AssetFormat<U>> formats = Lists.newArrayList();
    private List<AssetAlterationFormat<U>> deltaFormats = Lists.newArrayList();
    private List<AssetAlterationFormat<U>> supplementFormats = Lists.newArrayList();

    private Map<ResourceUrn, UnloadedAsset<U>> unloadedAssetLookup = Maps.newHashMap();
    private ListMultimap<ResourceUrn, UnloadedAssetAlteration<U>> assetDeltaLookup = ArrayListMultimap.create();
    private Map<ResourceUrn, ResourceUrn> redirectMap = Maps.newHashMap();
    private SetMultimap<Name, Name> resolutionMap = HashMultimap.create();

    public ModuleAssetProducer(Class<U> assetDataClass, String folderName) {
        Preconditions.checkNotNull(assetDataClass, "assetDataClass must not be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(folderName), "folderName must not be null or empty");
        this.assetDataClass = assetDataClass;
        this.folderName = folderName;
    }

    public void scan(ModuleEnvironment environment) {
        Preconditions.checkNotNull(environment);
        this.moduleEnvironment = environment;

        unloadedAssetLookup.clear();
        resolutionMap.clear();
        assetDeltaLookup.clear();
        redirectMap.clear();
        scanForAssets();
        Map<ResourceUrn, Name> overriddenByModule = scanForOverrides();
        scanForDeltas(overriddenByModule);
        scanForRedirects();
    }

    @Override
    public Set<ResourceUrn> resolve(String urn, Name moduleContext) {
        final Name resourceName = new Name(urn);
        Set<Name> supplyingModules = resolutionMap.get(resourceName);
        if (moduleContext != null && !moduleContext.isEmpty()) {
            if (supplyingModules.contains(moduleContext)) {
                return ImmutableSet.of(new ResourceUrn(moduleContext, resourceName));
            }
            Set<ResourceUrn> resources = Sets.newLinkedHashSet();
            for (Name dependency : moduleEnvironment.getDependencyNamesOf(moduleContext)) {
                if (supplyingModules.contains(dependency)) {
                    resources.add(new ResourceUrn(dependency, resourceName));
                }
            }
            if (!resources.isEmpty()) {
                return resources;
            }
        }

        return Sets.newLinkedHashSet(Collections2.transform(supplyingModules, new Function<Name, ResourceUrn>() {
            @Nullable
            @Override
            public ResourceUrn apply(Name moduleName) {
                return new ResourceUrn(moduleName, resourceName);
            }
        }));
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
    public U getAssetData(ResourceUrn urn) throws IOException {
        if (urn.getFragmentName().isEmpty()) {
            UnloadedAsset<U> source = unloadedAssetLookup.get(urn);
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

    public void removeFormat(AssetFormat<U> format) {
        formats.remove(format);
    }

    public void removeAllFormats() {
        formats.clear();
    }

    public List<AssetAlterationFormat<U>> getDeltaFormats() {
        return Collections.unmodifiableList(deltaFormats);
    }

    public void addDeltaFormat(AssetAlterationFormat<U> format) {
        deltaFormats.add(format);
    }

    public void removeDeltaFormat(AssetAlterationFormat<U> format) {
        deltaFormats.remove(format);
    }

    public void removeAllDeltaFormats() {
        deltaFormats.clear();
    }

    public void addSupplementFormat(AssetAlterationFormat<U> format) {
        supplementFormats.add(format);
    }

    public List<AssetAlterationFormat<U>> getSupplementFormats() {
        return Collections.unmodifiableList(supplementFormats);
    }

    public void removeSupplementFormat(AssetAlterationFormat<U> format) {
        supplementFormats.remove(format);
    }

    public void removeAllSupplementFormats() {
        supplementFormats.clear();
    }

    private void scanForAssets() {
        for (Module module : moduleEnvironment) {
            ModuleNameProvider moduleNameProvider = new FixedModuleNameProvider(module.getId());
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, ASSET_FOLDER, folderName);
            if (Files.exists(rootPath)) {
                Map<ResourceUrn, UnloadedAsset<U>> moduleSources = Maps.newLinkedHashMap();
                for (AssetFormat<U> format : formats) {
                    moduleSources.putAll(scanForAssets(format, module, rootPath, moduleNameProvider, format.getFileMatcher()));
                }
                for (AssetAlterationFormat<U> format : supplementFormats) {
                    scanForAssetSupplements(format, module, rootPath, moduleNameProvider, format.getFileMatcher(), moduleSources);
                }
                for (ResourceUrn urn : moduleSources.keySet()) {
                    resolutionMap.put(urn.getResourceName(), urn.getModuleName());
                }
                unloadedAssetLookup.putAll(moduleSources);
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
                    moduleOverrides.putAll(scanForAssets(format, module, rootPath, new PathModuleNameProvider(1), new FormatMatcher(folderName, format.getFileMatcher())));
                }
                for (AssetAlterationFormat<U> format : supplementFormats) {
                    scanForAssetSupplements(format, module, rootPath, new PathModuleNameProvider(1), new FormatMatcher(folderName, format.getFileMatcher()), moduleOverrides);
                }

                Set<Name> moduleDependencies = moduleEnvironment.getDependencyNamesOf(module.getId());
                for (Map.Entry<ResourceUrn, UnloadedAsset<U>> entry : moduleOverrides.entrySet()) {
                    if (!moduleDependencies.contains(entry.getKey().getModuleName())) {
                        logger.warn("Module '{}' contains overrides for non-dependency '{}', skipping", module.getId(), entry.getKey().getModuleName());
                        continue;
                    }
                    Name oldModule = overriddenByModule.put(entry.getKey(), module.getId());
                    if (oldModule != null && !moduleDependencies.contains(oldModule)) {
                        logger.warn("Conflicting overrides of '{}', applying from '{}' over '{}'", entry.getKey(), module.getId(), oldModule);
                    }
                    unloadedAssetLookup.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return overriddenByModule;
    }

    private Map<ResourceUrn, UnloadedAsset<U>> scanForAssets(AssetFormat<U> format, Module origin, Path rootPath,
                                                             ModuleNameProvider moduleNameProvider, PathMatcher pathMatcher) {
        Map<ResourceUrn, UnloadedAsset<U>> results = Maps.newLinkedHashMap();
        try {
            for (Path file : origin.findFiles(rootPath, FileScanning.acceptAll(), pathMatcher)) {
                try {
                    Name assetName = format.getAssetName(file.getFileName().toString());
                    Name moduleName = moduleNameProvider.getModuleName(file);
                    ResourceUrn urn = new ResourceUrn(moduleName, assetName);
                    UnloadedAsset<U> source = results.get(urn);
                    if (source == null) {
                        source = new UnloadedAsset<>(urn, format);
                        results.put(urn, source);
                    }
                    source.addInput(file);
                } catch (InvalidAssetFilenameException e) {
                    logger.error("Invalid file name '{}' for asset data type '{}", file.getFileName(), assetDataClass.getSimpleName(), e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan for assets of '{}' in 'module://{}:{}", assetDataClass, origin.getId(), rootPath, e);
        }
        return results;
    }

    private void scanForAssetSupplements(AssetAlterationFormat<U> format, Module module, Path rootPath, ModuleNameProvider moduleNameProvider,
                                         PathMatcher pathMatcher, Map<ResourceUrn, UnloadedAsset<U>> primarySources) {
        Map<ResourceUrn, UnloadedAssetAlteration<U>> supplementSources = Maps.newLinkedHashMap();
        try {
            for (Path file : module.findFiles(rootPath, FileScanning.acceptAll(), pathMatcher)) {
                try {
                    Name assetName = format.getAssetName(file.getFileName().toString());
                    ResourceUrn urn = new ResourceUrn(moduleNameProvider.getModuleName(file), assetName);
                    UnloadedAssetAlteration<U> source = supplementSources.get(urn);
                    if (source == null) {
                        source = new UnloadedAssetAlteration<>(format);
                        supplementSources.put(urn, source);
                    }
                    source.addInput(file);
                } catch (InvalidAssetFilenameException e) {
                    logger.error("Invalid file name '{}' for asset supplement for '{}'", file.getFileName(), assetDataClass, e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan 'module://{}:{}' for asset supplements", module.getId(), rootPath, e);
        }

        for (Map.Entry<ResourceUrn, UnloadedAssetAlteration<U>> supplementEntry : supplementSources.entrySet()) {
            UnloadedAsset<U> unloadedAsset = primarySources.get(supplementEntry.getKey());
            if (unloadedAsset != null) {
                unloadedAsset.addAlteration(supplementEntry.getValue());
            } else {
                logger.error("Found supplement for unknown asset '{}'", supplementEntry.getKey());
            }
        }
    }

    private void scanForDeltas(Map<ResourceUrn, Name> overriddenByModule) {
        for (Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, DELTA_FOLDER);
            if (Files.exists(rootPath)) {
                for (AssetAlterationFormat<U> format : deltaFormats) {
                    for (Map.Entry<ResourceUrn, UnloadedAssetAlteration<U>> delta : scanForDeltasOfFormat(module, rootPath, format).entrySet()) {
                        Name overrideModule = overriddenByModule.get(delta.getKey());
                        if (overrideModule != null && moduleEnvironment.getDependencyNamesOf(overrideModule).contains(delta.getKey().getModuleName())) {
                            continue;
                        }

                        UnloadedAsset<U> asset = unloadedAssetLookup.get(delta.getKey());
                        if (asset != null) {
                            asset.addAlteration(delta.getValue());
                        } else {
                            logger.warn("Discovered delta for unknown asset '{}'", delta.getKey());
                        }
                    }
                }
            }
        }
    }

    private Map<ResourceUrn, UnloadedAssetAlteration<U>> scanForDeltasOfFormat(Module origin, Path rootPath, AssetAlterationFormat<U> format) {
        Map<ResourceUrn, UnloadedAssetAlteration<U>> discoveredDeltas = Maps.newLinkedHashMap();
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
                    UnloadedAssetAlteration<U> source = discoveredDeltas.get(urn);
                    if (source == null) {
                        source = new UnloadedAssetAlteration<>(format);
                        discoveredDeltas.put(urn, source);
                    }
                    source.addInput(file);
                } catch (InvalidAssetFilenameException e) {
                    logger.error("Invalid file name '{}' for asset delta for asset data type '{}", file.getFileName(), assetDataClass, e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan for deltas of '{}'", assetDataClass, e);
        }
        return discoveredDeltas;
    }

    private void scanForRedirects() {
        Map<ResourceUrn, ResourceUrn> rawRedirects = Maps.newLinkedHashMap();
        for (Module module : moduleEnvironment) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, ASSET_FOLDER, folderName);
            if (Files.exists(rootPath)) {
                try {
                    for (Path file : module.findFiles(rootPath, FileScanning.acceptAll(), new FileExtensionPathMatcher(REDIRECT_EXTENSION))) {
                        Name assetName = new Name(com.google.common.io.Files.getNameWithoutExtension(file.getFileName().toString()));
                        try (BufferedReader reader = Files.newBufferedReader(file, Charsets.UTF_8)) {
                            List<String> contents = CharStreams.readLines(reader);
                            if (contents.isEmpty()) {
                                logger.error("Failed to read redirect '{}:{}' - empty", module.getId(), assetName);
                            } else if (!ResourceUrn.isValid(contents.get(0))) {
                                logger.error("Failed to read redirect '{}:{}' - '{}' is not a valid urn", module.getId(), assetName, contents.get(0));
                            } else {
                                rawRedirects.put(new ResourceUrn(module.getId(), assetName), new ResourceUrn(contents.get(0)));
                                resolutionMap.put(assetName, module.getId());
                            }
                        } catch (IOException e) {
                            logger.error("Failed to read redirect '{}:{}'", module.getId(), assetName, e);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed to scan module '{}' for assets", module.getId(), e);
                }
            }
        }

        for (Map.Entry<ResourceUrn, ResourceUrn> entry : rawRedirects.entrySet()) {
            ResourceUrn currentTarget = entry.getKey();
            ResourceUrn redirect = entry.getValue();
            while (redirect != null) {
                currentTarget = redirect;
                redirect = rawRedirects.get(currentTarget);
            }
            redirectMap.put(entry.getKey(), currentTarget);
        }
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

    private interface ModuleNameProvider {
        Name getModuleName(Path file);
    }

    private static class FixedModuleNameProvider implements ModuleNameProvider {
        private Name moduleName;

        public FixedModuleNameProvider(Name name) {
            this.moduleName = name;
        }

        @Override
        public Name getModuleName(Path file) {
            return moduleName;
        }
    }

    private static class PathModuleNameProvider implements ModuleNameProvider {
        private int nameIndex;

        public PathModuleNameProvider(int index) {
            this.nameIndex = index;
        }

        @Override
        public Name getModuleName(Path file) {
            return new Name(file.getName(nameIndex).toString());
        }
    }
}
