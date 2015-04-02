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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
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
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.filesystem.ModuleFileSystemProvider;
import org.terasology.naming.Name;
import org.terasology.util.io.FileExtensionPathMatcher;
import org.terasology.util.io.FileScanning;

import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
public class ModuleAssetDataProducer<U extends AssetData> implements AssetDataProducer<U>, Closeable {

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
    public static final String REDIRECT_EXTENSION = "redirect";

    private static final Logger logger = LoggerFactory.getLogger(ModuleAssetDataProducer.class);

    private final String folderName;

    private final ModuleEnvironment moduleEnvironment;

    private final ImmutableList<AssetFileFormat<U>> assetFormats;
    private final ImmutableList<AssetAlterationFileFormat<U>> deltaFormats;
    private final ImmutableList<AssetAlterationFileFormat<U>> supplementFormats;

    private final Map<ResourceUrn, UnloadedAssetData<U>> unloadedAssetLookup = new MapMaker().concurrencyLevel(1).makeMap();
    private final ImmutableMap<ResourceUrn, ResourceUrn> redirectMap;
    private final SetMultimap<Name, Name> resolutionMap = Multimaps.synchronizedSetMultimap(HashMultimap.<Name, Name>create());

    private final ImmutableList<WatchService> moduleWatchServices;
    private final Map<WatchKey, PathWatcher> pathWatchers = new MapMaker().concurrencyLevel(1).makeMap();
    private final Map<Path, WatchKey> watchKeys = new MapMaker().concurrencyLevel(1).makeMap();

    /**
     * Creates a ModuleAssetDataProducer
     *
     * @param folderName          The subfolder that contains files relevant to the asset data this producer loads
     * @param moduleEnvironment   The module environment to load asset data from
     * @param assetFormats        The file formats supported for loading asset files
     * @param supplementalFormats The supplementary file formats supported when loading asset files
     * @param deltaFormats        The delta file formats supported when loading asset files
     */
    public ModuleAssetDataProducer(String folderName,
                                   ModuleEnvironment moduleEnvironment,
                                   Collection<AssetFileFormat<U>> assetFormats,
                                   Collection<AssetAlterationFileFormat<U>> supplementalFormats,
                                   Collection<AssetAlterationFileFormat<U>> deltaFormats) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(folderName), "folderName must not be null or empty");
        this.folderName = folderName;
        this.moduleEnvironment = moduleEnvironment;
        this.assetFormats = ImmutableList.copyOf(assetFormats);
        this.supplementFormats = ImmutableList.copyOf(supplementalFormats);
        this.deltaFormats = ImmutableList.copyOf(deltaFormats);

        scanForAssets();
        scanForOverrides();
        scanForDeltas();
        redirectMap = buildRedirectMap(scanModulesForRedirects());

        moduleWatchServices = startWatchService();
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
     * Closes the producer, shutting down the watch service that observes the file system for changes
     */
    @Override
    public synchronized void close() {
        shutdownWatchService();
        unloadedAssetLookup.clear();
        resolutionMap.clear();
    }

    /**
     * @return The module environment that asset data is read from
     */
    public ModuleEnvironment getModuleEnvironment() {
        return moduleEnvironment;
    }

    /**
     * Checks the file system for any changes that affects assets.
     *
     * @return A set of ResourceUrns of changed assets.
     */
    public synchronized Set<ResourceUrn> checkForChanges() {
        Set<ResourceUrn> changed = Sets.newLinkedHashSet();
        for (WatchService service : moduleWatchServices) {
            WatchKey key = service.poll();
            while (key != null) {
                PathWatcher pathWatcher = pathWatchers.get(key);
                changed.addAll(pathWatcher.update(key.pollEvents()));
                key.reset();
                key = service.poll();
            }
        }
        return changed;
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
        return Optional.absent();
    }

    private void scanForAssets() {
        for (Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, ASSET_FOLDER, folderName);
            if (Files.exists(rootPath)) {
                scanLocationForAssets(module, rootPath, path -> module.getId());
            }
        }
    }

    private void scanForOverrides() {
        for (Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, OVERRIDE_FOLDER);
            if (Files.exists(rootPath)) {
                scanLocationForAssets(module, rootPath, path -> new Name(path.getName(1).toString()));
            }
        }
    }

    private void scanLocationForAssets(final Module origin, Path rootPath, Function<Path, Name> moduleNameProvider) {
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Name module = moduleNameProvider.apply(file);
                    Optional<ResourceUrn> assetUrn = registerSource(module, file, origin.getId(), assetFormats, UnloadedAssetData::addSource);
                    if (!assetUrn.isPresent()) {
                        registerSource(moduleNameProvider.apply(file), file, origin.getId(), supplementFormats, UnloadedAssetData::addSupplementSource);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Failed to scan for assets of '{}' in 'module://{}:{}", folderName, origin.getId(), rootPath, e);
        }
    }

    private void scanForDeltas() {
        for (final Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, DELTA_FOLDER);
            if (Files.exists(rootPath)) {
                try {
                    Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            registerAssetDelta(new Name(file.getName(1).toString()), file, module.getId());
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    logger.error("Failed to scan for asset deltas of '{}' in 'module://{}:{}", folderName, module.getId(), rootPath, e);
                }
            }
        }
    }


    private Map<ResourceUrn, ResourceUrn> scanModulesForRedirects() {
        Map<ResourceUrn, ResourceUrn> rawRedirects = Maps.newLinkedHashMap();
        for (Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            Path rootPath = module.getFileSystem().getPath(ModuleFileSystemProvider.ROOT, ASSET_FOLDER, folderName);
            if (Files.exists(rootPath)) {
                try {
                    for (Path file : module.findFiles(rootPath, FileScanning.acceptAll(), new FileExtensionPathMatcher(REDIRECT_EXTENSION))) {
                        processRedirectFile(file, module.getId(), rawRedirects);
                    }
                } catch (IOException e) {
                    logger.error("Failed to scan module '{}' for assets", module.getId(), e);
                }
            }
        }
        return rawRedirects;
    }

    private ImmutableMap<ResourceUrn, ResourceUrn> buildRedirectMap(Map<ResourceUrn, ResourceUrn> rawRedirects) {
        ImmutableMap.Builder<ResourceUrn, ResourceUrn> builder = ImmutableMap.builder();
        for (Map.Entry<ResourceUrn, ResourceUrn> entry : rawRedirects.entrySet()) {
            ResourceUrn currentTarget = entry.getKey();
            ResourceUrn redirect = entry.getValue();
            while (redirect != null) {
                currentTarget = redirect;
                redirect = rawRedirects.get(currentTarget);
            }
            builder.put(entry.getKey(), currentTarget);
        }
        return builder.build();
    }

    private void processRedirectFile(Path file, Name moduleId, Map<ResourceUrn, ResourceUrn> rawRedirects) {
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
                    rawRedirects.put(new ResourceUrn(moduleId, assetName), new ResourceUrn(contents.get(0)));
                    resolutionMap.put(assetName, moduleId);
                }
            } catch (IOException e) {
                logger.error("Failed to read redirect '{}:{}'", moduleId, assetName, e);
            }
        } else {
            logger.error("Missing file name for redirect");
        }
    }

    private <V extends FileFormat> Optional<ResourceUrn> registerSource(Name module, Path target, Name providingModule,
                                                                        Collection<V> formats, RegisterSourceHandler<U, V> sourceHandler) {
        Path filename = target.getFileName();
        if (filename == null) {
            logger.error("Missing filename for asset file");
            return Optional.absent();
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
                    return Optional.absent();
                } catch (InvalidAssetFilenameException e) {
                    logger.warn("Invalid name for asset - {}", filename);
                }
            }
        }
        return Optional.absent();
    }

    private Optional<ResourceUrn> registerAssetDelta(Name module, Path target, Name providingModule) {
        Path filename = target.getFileName();
        if (filename == null) {
            logger.error("Missing file name for asset delta for '{}'", folderName);
            return Optional.absent();
        }
        for (AssetAlterationFileFormat<U> format : deltaFormats) {
            if (format.getFileMatcher().matches(target)) {
                try {
                    Name assetName = format.getAssetName(filename.toString());
                    ResourceUrn urn = new ResourceUrn(module, assetName);
                    UnloadedAssetData<U> unloadedAssetData = unloadedAssetLookup.get(urn);
                    if (unloadedAssetData == null) {
                        logger.warn("Discovered delta for unknown asset '{}'", urn);
                        return Optional.absent();
                    }
                    if (unloadedAssetData.addDeltaSource(providingModule, format, target)) {
                        return Optional.of(urn);
                    }
                } catch (InvalidAssetFilenameException e) {
                    logger.error("Invalid file name '{}' for asset delta for '{}'", target.getFileName(), folderName, e);
                }
            }
        }
        return Optional.absent();
    }

    private ImmutableList<WatchService> startWatchService() {
        ImmutableList.Builder<WatchService> watchServiceBuilder = ImmutableList.builder();
        for (Module module : moduleEnvironment.getModulesOrderedByDependencies()) {
            try {
                final WatchService moduleWatchService = module.getFileSystem().newWatchService();
                watchServiceBuilder.add(moduleWatchService);

                for (Path rootPath : module.getFileSystem().getRootDirectories()) {
                    PathWatcher watcher = new RootPathWatcher(rootPath, module.getId(), moduleWatchService);
                    watcher.onRegistered();
                }
            } catch (IOException e) {
                logger.warn("Failed to establish change watch service for module '{}'", module, e);
            }
        }
        return watchServiceBuilder.build();
    }

    private void shutdownWatchService() {
        for (WatchService watchService : moduleWatchServices) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Failed to shutdown watch service", e);
            }
        }
        pathWatchers.clear();
        watchKeys.clear();
    }

    /**
     * A PathWatcher watches a path for changes, and reacts to those changes.
     */
    private abstract class PathWatcher {
        private Path watchedPath;
        private WatchService watchService;

        public PathWatcher(Path path, WatchService watchService) throws IOException {
            this.watchedPath = path;
            this.watchService = watchService;
            WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            if (key.isValid()) {
                pathWatchers.put(key, this);
                watchKeys.put(path, key);
            }
        }

        public Path getWatchedPath() {
            return watchedPath;
        }

        public WatchService getWatchService() {
            return watchService;
        }

        @SuppressWarnings("unchecked")
        public final Set<ResourceUrn> update(List<WatchEvent<?>> watchEvents) {
            final Set<ResourceUrn> changedAssets = Sets.newLinkedHashSet();
            for (WatchEvent<?> event : watchEvents) {
                WatchEvent.Kind kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    logger.warn("File event overflow - lost change events");
                    continue;
                }

                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path target = watchedPath.resolve(pathEvent.context());
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(target)) {
                        onDirectoryCreated(target, changedAssets);
                    } else {
                        onFileCreated(target, changedAssets);
                    }
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    if (Files.isRegularFile(target)) {
                        onFileModified(target, changedAssets);
                    }
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    WatchKey key = watchKeys.remove(target);
                    if (key != null) {
                        pathWatchers.remove(key);
                    } else {
                        onFileDeleted(target, changedAssets);
                    }
                }
            }
            return changedAssets;
        }

        private void onDirectoryCreated(Path target, Set<ResourceUrn> outChanged) {
            try {
                Optional<? extends PathWatcher> pathWatcher = processPath(target);
                if (pathWatcher.isPresent()) {
                    pathWatcher.get().onCreated(outChanged);
                }
            } catch (IOException e) {
                logger.error("Error registering path for change watching '{}'", getWatchedPath(), e);
            }
        }

        /**
         * Called when the path watcher is registered for an existing path
         */
        public final void onRegistered() {
            try (DirectoryStream<Path> contents = Files.newDirectoryStream(getWatchedPath())) {
                for (Path path : contents) {
                    if (Files.isDirectory(path)) {
                        Optional<? extends PathWatcher> pathWatcher = processPath(path);
                        if (pathWatcher.isPresent()) {
                            pathWatcher.get().onRegistered();
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error registering path for change watching '{}'", getWatchedPath(), e);
            }
        }

        /**
         * Called when the path watcher is for a newly created path
         *
         * @param outChanged The ResourceUrns of any assets affected by the creation of this path
         */
        public final void onCreated(Set<ResourceUrn> outChanged) {
            try (DirectoryStream<Path> contents = Files.newDirectoryStream(getWatchedPath())) {
                for (Path path : contents) {
                    if (Files.isDirectory(path)) {
                        onDirectoryCreated(path, outChanged);
                    } else {
                        onFileCreated(path, outChanged);
                    }
                }
            } catch (IOException e) {
                logger.error("Error registering path for change watching '{}'", getWatchedPath(), e);
            }
        }

        /**
         * Processes a path within this path watcher
         *
         * @param target The path to process
         * @return A new path watcher for the path
         * @throws IOException If there was any issue processing the path
         */
        protected abstract Optional<? extends PathWatcher> processPath(Path target) throws IOException;

        /**
         * Called when a file is created
         *
         * @param target     The created file
         * @param outChanged The ResourceUrns of any assets affected
         */
        protected void onFileCreated(Path target, final Set<ResourceUrn> outChanged) {
        }

        /**
         * Called when a file is modified
         *
         * @param target     The modified file
         * @param outChanged The ResourceUrns of any assets affected
         */
        protected void onFileModified(Path target, final Set<ResourceUrn> outChanged) {
        }

        /**
         * Called when a file is deleted
         *
         * @param target     The deleted file
         * @param outChanged The ResourceUrns of any assets affected
         */
        protected void onFileDeleted(Path target, final Set<ResourceUrn> outChanged) {
        }
    }

    private class RootPathWatcher extends PathWatcher {

        private Name module;

        public RootPathWatcher(Path path, Name module, WatchService watchService) throws IOException {
            super(path, watchService);
            this.module = module;
        }

        @Override
        protected Optional<? extends PathWatcher> processPath(Path target) throws IOException {
            if (target.getNameCount() == 1) {
                switch (target.getName(0).toString()) {
                    case ASSET_FOLDER: {
                        return Optional.of(new AssetRootPathWatcher(target, module, getWatchService()));
                    }
                    case DELTA_FOLDER: {
                        return Optional.of(new DeltaRootPathWatcher(target, module, getWatchService()));
                    }
                    case OVERRIDE_FOLDER: {
                        return Optional.of(new OverrideRootPathWatcher(target, module, getWatchService()));
                    }
                }
            }
            return Optional.absent();
        }
    }

    private class AssetRootPathWatcher extends PathWatcher {

        private Name module;

        public AssetRootPathWatcher(Path path, Name module, WatchService watchService) throws IOException {
            super(path, watchService);
            this.module = module;
        }

        @Override
        protected Optional<? extends PathWatcher> processPath(Path target) throws IOException {
            if (target.getNameCount() == 2 && target.getName(1).toString().equals(folderName)) {
                return Optional.of(new AssetPathWatcher(target, module, module, getWatchService()));
            }
            return Optional.absent();
        }
    }

    private class OverrideRootPathWatcher extends PathWatcher {

        private Name module;

        public OverrideRootPathWatcher(Path path, Name module, WatchService watchService) throws IOException {
            super(path, watchService);
            this.module = module;
        }

        @Override
        protected Optional<? extends PathWatcher> processPath(Path target) throws IOException {
            if (target.getNameCount() == 2) {
                return Optional.of(new OverrideRootPathWatcher(target, module, getWatchService()));
            } else if (target.getNameCount() == 3) {
                return Optional.of(new AssetPathWatcher(target, new Name(target.getName(1).toString()), module, getWatchService()));
            }
            return Optional.absent();
        }
    }

    private class DeltaRootPathWatcher extends PathWatcher {

        private Name module;

        public DeltaRootPathWatcher(Path path, Name module, WatchService watchService) throws IOException {
            super(path, watchService);
            this.module = module;
        }

        @Override
        protected Optional<? extends PathWatcher> processPath(Path target) throws IOException {
            if (target.getNameCount() == 2) {
                return Optional.of(new DeltaRootPathWatcher(target, module, getWatchService()));
            } else if (target.getNameCount() == 3) {
                return Optional.of(new DeltaPathWatcher(target, new Name(target.getName(1).toString()), module, getWatchService()));
            }
            return Optional.absent();
        }
    }

    private class DeltaPathWatcher extends PathWatcher {

        private final Name providingModule;
        private final Name module;

        public DeltaPathWatcher(Path path, Name module, Name providingModule, WatchService watchService) throws IOException {
            super(path, watchService);
            this.module = module;
            this.providingModule = providingModule;
        }

        @Override
        protected Optional<? extends PathWatcher> processPath(Path target) throws IOException {
            return Optional.of(new DeltaPathWatcher(target, module, providingModule, getWatchService()));
        }

        private ResourceUrn getResourceUrn(Path target) {
            Path filename = target.getFileName();
            if (filename == null) {
                logger.error("Missing filename for resource target");
                return null;
            }

            for (FileFormat fileFormat : deltaFormats) {
                if (fileFormat.getFileMatcher().matches(target)) {
                    try {
                        Name assetName = fileFormat.getAssetName(filename.toString());
                        return new ResourceUrn(module, assetName);
                    } catch (InvalidAssetFilenameException e) {
                        logger.debug("Modified file does not have a valid asset name - '{}'", filename);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onFileCreated(Path target, Set<ResourceUrn> outChanged) {
            Optional<ResourceUrn> urn = registerAssetDelta(module, target, providingModule);
            if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
                outChanged.add(urn.get());
            }
        }

        @Override
        protected void onFileModified(Path target, Set<ResourceUrn> outChanged) {
            ResourceUrn urn = getResourceUrn(target);
            if (urn != null) {
                if (unloadedAssetLookup.get(urn).isValid()) {
                    outChanged.add(urn);
                }
            }
        }

        @Override
        protected void onFileDeleted(Path target, Set<ResourceUrn> outChanged) {
            Path filename = target.getFileName();
            if (filename == null) {
                logger.error("Missing filename for deleted file");
                return;
            }
            for (AssetAlterationFileFormat<U> format : deltaFormats) {
                if (format.getFileMatcher().matches(target)) {
                    try {
                        Name assetName = format.getAssetName(filename.toString());
                        ResourceUrn urn = new ResourceUrn(module, assetName);
                        UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                        if (existing != null) {
                            existing.removeDeltaSource(providingModule, format, target);
                            if (existing.isValid()) {
                                outChanged.add(urn);
                            }
                        }
                        return;
                    } catch (InvalidAssetFilenameException e) {
                        logger.debug("Deleted file does not have a valid file name - {}", target);
                    }
                }
            }
        }
    }

    private class AssetPathWatcher extends PathWatcher {

        private Name module;
        private Name providingModule;

        public AssetPathWatcher(Path path, Name module, Name providingModule, WatchService watchService) throws IOException {
            super(path, watchService);
            this.module = module;
            this.providingModule = providingModule;
        }

        @Override
        protected Optional<? extends PathWatcher> processPath(Path target) throws IOException {
            return Optional.of(new AssetPathWatcher(target, module, providingModule, getWatchService()));
        }

        private ResourceUrn getResourceUrn(Path target, Collection<? extends FileFormat> formats) {
            Path filename = target.getFileName();
            if (filename != null) {
                for (FileFormat fileFormat : formats) {
                    if (fileFormat.getFileMatcher().matches(target)) {
                        try {
                            Name assetName = fileFormat.getAssetName(filename.toString());
                            return new ResourceUrn(module, assetName);
                        } catch (InvalidAssetFilenameException e) {
                            logger.debug("Modified file does not have a valid asset name - '{}'", filename);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onFileCreated(Path target, Set<ResourceUrn> outChanged) {
            Optional<ResourceUrn> urn = registerSource(module, target, providingModule, assetFormats, UnloadedAssetData::addSource);
            if (!urn.isPresent()) {
                urn = registerSource(module, target, providingModule, supplementFormats, UnloadedAssetData::addSupplementSource);
            }
            if (urn.isPresent() && unloadedAssetLookup.get(urn.get()).isValid()) {
                outChanged.add(urn.get());
            }
        }

        @Override
        protected void onFileModified(Path target, Set<ResourceUrn> outChanged) {
            ResourceUrn urn = getResourceUrn(target, assetFormats);
            if (urn == null) {
                urn = getResourceUrn(target, supplementFormats);
            }
            if (urn != null && unloadedAssetLookup.get(urn).isValid()) {
                outChanged.add(urn);
            }
        }

        @Override
        protected void onFileDeleted(Path target, Set<ResourceUrn> outChanged) {
            Path filename = target.getFileName();
            if (filename != null) {
                for (AssetFileFormat<U> format : assetFormats) {
                    if (format.getFileMatcher().matches(target)) {
                        try {
                            Name assetName = format.getAssetName(filename.toString());
                            ResourceUrn urn = new ResourceUrn(module, assetName);
                            UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                            if (existing != null) {
                                existing.removeSource(providingModule, format, target);
                                if (existing.isValid()) {
                                    outChanged.add(urn);
                                }
                            }
                            return;
                        } catch (InvalidAssetFilenameException e) {
                            logger.debug("Deleted file does not have a valid file name - {}", target);
                        }
                    }
                }
                for (AssetAlterationFileFormat<U> format : supplementFormats) {
                    if (format.getFileMatcher().matches(target)) {
                        try {
                            Name assetName = format.getAssetName(filename.toString());
                            ResourceUrn urn = new ResourceUrn(module, assetName);
                            UnloadedAssetData<U> existing = unloadedAssetLookup.get(urn);
                            if (existing != null) {
                                existing.removeSupplementSource(providingModule, format, target);
                                if (existing.isValid()) {
                                    outChanged.add(urn);
                                }
                            }
                            return;
                        } catch (InvalidAssetFilenameException e) {
                            logger.debug("Deleted file does not have a valid file name - {}", target);
                        }
                    }
                }
            }
        }
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
