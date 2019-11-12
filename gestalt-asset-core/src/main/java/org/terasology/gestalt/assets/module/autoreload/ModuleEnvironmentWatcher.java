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

package org.terasology.gestalt.assets.module.autoreload;

import android.support.annotation.RequiresApi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.format.producer.FileChangeSubscriber;
import org.terasology.gestalt.assets.module.ModuleAssetScanner;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.module.resources.DirectoryFileSource;
import org.terasology.gestalt.module.resources.FileReference;
import org.terasology.gestalt.naming.Name;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;

/**
 * ModuleEnvironmentWatcher detects of changes to asset files (creation, modification, deletion), to inform ModuleAssetDataProducers of the changes
 * and trigger the reload of those assets.
 * <p>
 * One small note: Sometimes it is possible to get file change notifications before the change has actually happened. I observed this under windows
 * when using the New->Folder/New->File optional in explorer - the path will report as not existing unless you wait a short period of time. To address
 * this those events are added into an unreadyEvents queue and processed next check.
 * </p>
 *
 * @author Immortius
 */
@RequiresApi(26)
class ModuleEnvironmentWatcher {

    private static final Logger logger = LoggerFactory.getLogger(ModuleEnvironmentWatcher.class);

    private final WatchService service;

    private final Map<WatchKey, PathWatcher> pathWatchers = new MapMaker().concurrencyLevel(1).makeMap();
    private final Map<Path, WatchKey> watchKeys = new MapMaker().concurrencyLevel(1).makeMap();
    private final ListMultimap<String, SubscriberInfo> subscribers = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    private final BlockingDeque<DelayedEvent> unreadyEvents = Queues.newLinkedBlockingDeque();

    private boolean closed;

    /**
     * Creates a ModuleWatcher for the given module environment
     *
     * @param environment The environment to watch.
     * @throws IOException If there is an issue establishing the watch service
     */
    ModuleEnvironmentWatcher(ModuleEnvironment environment) throws IOException {
        this(environment, FileSystems.getDefault());
    }

    ModuleEnvironmentWatcher(ModuleEnvironment environment, FileSystem fileSystem) throws IOException {
        this.service = fileSystem.newWatchService();

        for (Module module : environment.getModulesOrderedByDependencies()) {
            for (Path path : module.getResources().getRootPaths()) {
                PathWatcher watcher = new PathWatcher(path, path, service, new RootPathWatcher(module.getId()));
                watcher.onRegistered();
            }
        }
    }

    /**
     * Registers an subscriber to a specific asset folder, for a given asset type.
     *
     * @param folderName The name of the folder to subscribe to - under which assets of interest lie (e.g. "textures")
     * @param subscriber The subscriber to notify of changes
     * @param assetType  The asset type whom changes urns belong to
     */
    synchronized void register(String folderName, FileChangeSubscriber subscriber, AssetType<?, ?> assetType) {
        Preconditions.checkState(!closed, "Cannot register folder into closed ModuleWatcher");
        subscribers.put(folderName, new SubscriberInfo(assetType, subscriber));
    }

    /**
     * Unregisters all subscribes for a given asset type
     *
     * @param assetType The assetType to remove subscribers for
     */
    synchronized void unregister(AssetType<?, ?> assetType) {
        Preconditions.checkState(!closed, "ModuleWatcher is closed");
        subscribers.values().removeIf(v -> v.type.equals(assetType));
    }

    /**
     * @return Whether the ModuleEnvironmentWatcher is closed
     */
    synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Shuts down the ModuleWatcher.
     *
     * @throws IOException If there is an error shutting down the service
     */
    synchronized void shutdown() throws IOException {
        if (!closed) {
            pathWatchers.clear();
            watchKeys.clear();
            service.close();
            closed = true;
        }
    }

    /**
     * Checks the file system for any changes that affects assets.
     *
     * @return A set of ResourceUrns of changed assets.
     */
    synchronized SetMultimap<AssetType<?, ?>, ResourceUrn> checkForChanges() {
        if (closed) {
            return LinkedHashMultimap.create();
        }

        SetMultimap<AssetType<?, ?>, ResourceUrn> changed = LinkedHashMultimap.create();

        List<DelayedEvent> events = Lists.newArrayList();
        unreadyEvents.drainTo(events);
        for (DelayedEvent event : events) {
            changed.putAll(event.replay());
        }

        WatchKey key = service.poll();
        while (key != null) {
            PathWatcher pathWatcher = pathWatchers.get(key);
            changed.putAll(pathWatcher.update(key.pollEvents(), unreadyEvents));
            key.reset();
            key = service.poll();
        }
        return changed;
    }

    /**
     * Notifies subscribers of an asset file change
     *
     * @param folderName      The asset folder in which the changed asset resides
     * @param file            The file tha changed
     * @param module          The module the file contributes to
     * @param providingModule The module that provides the file
     * @param method          The subscription method to call to notify
     * @param outChanged      A map of asset types and their changed urns to add any modified resource urns to.
     */
    private void notifySubscribers(String folderName, FileReference file, Name module, Name providingModule, SubscriptionMethod method,
                                   SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
        for (SubscriberInfo subscriber : subscribers.get(folderName)) {
            Optional<ResourceUrn> urn = method.notify(subscriber.subscriber, file, module, providingModule);
            urn.ifPresent(resourceUrn -> outChanged.put(subscriber.type, resourceUrn));
        }
    }

    /**
     * The form of a method to call to notify a subscriber of changes
     */
    private interface SubscriptionMethod {
        Optional<ResourceUrn> notify(FileChangeSubscriber subscriber, FileReference file, Name module, Name providingModule);
    }

    private interface PathChangeListener {
        /**
         * Processes a path, potentially returning a path change listener
         *
         * @param subpath The path to process
         * @return A new path watcher for the path
         * @throws IOException If there was any issue processing the path
         */
        Optional<? extends PathChangeListener> processPath(String subpath) throws IOException;

        /**
         * Called when a file is created
         *
         * @param target     The created file
         * @param outChanged The ResourceUrns of any assets affected
         */
        default void onFileCreated(FileReference target, final SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
        }

        /**
         * Called when a file is modified
         *
         * @param target     The modified file
         * @param outChanged The ResourceUrns of any assets affected
         */
        default void onFileModified(FileReference target, final SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
        }

        /**
         * Called when a file is deleted
         *
         * @param target     The deleted file
         * @param outChanged The ResourceUrns of any assets affected
         */
        default void onFileDeleted(FileReference target, final SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
        }
    }

    /**
     * Information on a subscriber.
     */
    private static class SubscriberInfo {
        final AssetType<?, ?> type;
        final FileChangeSubscriber subscriber;

        SubscriberInfo(AssetType<?, ?> type, FileChangeSubscriber subscriber) {
            this.type = type;
            this.subscriber = subscriber;
        }
    }

    private static class DelayedEvent {
        private WatchEvent<?> event;
        private PathWatcher watcher;

        DelayedEvent(WatchEvent<?> event, PathWatcher watcher) {
            this.event = event;
            this.watcher = watcher;
        }

        SetMultimap<AssetType<?, ?>, ResourceUrn> replay() {
            return watcher.update(Collections.singletonList(event), null);
        }
    }

    /**
     * A PathWatcher watches a path for changes, and reacts to those changes.
     */
    private final class PathWatcher {
        private Path watchedPath;
        private Path rootPath;
        private WatchService watchService;
        private PathChangeListener listener;

        private PathWatcher(Path path, Path rootPath, WatchService watchService, PathChangeListener listener) throws IOException {
            this.watchedPath = path;
            this.watchService = watchService;
            this.rootPath = rootPath;
            this.listener = listener;
            WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            if (key.isValid()) {
                pathWatchers.put(key, this);
                watchKeys.put(path, key);
            }
        }

        private Path getWatchedPath() {
            return watchedPath;
        }

        @SuppressWarnings("unchecked")
        private SetMultimap<AssetType<?, ?>, ResourceUrn> update(List<WatchEvent<?>> watchEvents, Collection<DelayedEvent> outDelayedEvents) {
            final SetMultimap<AssetType<?, ?>, ResourceUrn> changedAssets = LinkedHashMultimap.create();
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
                        logger.debug("New directory registered: {}", target);
                        onDirectoryCreated(target, changedAssets);
                    } else if (Files.isRegularFile(target)) {
                        logger.debug("New file registered: {}", target);
                        DirectoryFileSource.DirectoryFileReference file = new DirectoryFileSource.DirectoryFileReference(target.toFile(), rootPath.toFile());
                        listener.onFileCreated(file, changedAssets);
                    } else if (outDelayedEvents != null) {
                        outDelayedEvents.add(new DelayedEvent(event, this));
                    }
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    if (Files.isRegularFile(target)) {
                        logger.debug("File modified: {}", target);
                        DirectoryFileSource.DirectoryFileReference file = new DirectoryFileSource.DirectoryFileReference(target.toFile(), rootPath.toFile());
                        listener.onFileModified(file, changedAssets);
                    }
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    WatchKey key = watchKeys.remove(target);
                    if (key != null) {
                        pathWatchers.remove(key);
                    } else {
                        DirectoryFileSource.DirectoryFileReference file = new DirectoryFileSource.DirectoryFileReference(target.toFile(), rootPath.toFile());
                        listener.onFileDeleted(file, changedAssets);
                    }
                }
            }
            return changedAssets;
        }

        private void onDirectoryCreated(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            try {
                String relativePath = watchedPath.relativize(target).getName(0).toString();
                Optional<? extends PathChangeListener> pathChangeListener = listener.processPath(relativePath);
                if (pathChangeListener.isPresent()) {
                    new PathWatcher(target, rootPath, watchService, pathChangeListener.get()).onCreated(outChanged);
                }
            } catch (IOException e) {
                logger.error("Error registering path for change watching '{}'", getWatchedPath(), e);
            }
        }

        /**
         * Called when the path watcher is registered for an existing path
         */
        final void onRegistered() {
            try (DirectoryStream<Path> contents = Files.newDirectoryStream(getWatchedPath())) {
                for (Path path : contents) {
                    if (Files.isDirectory(path)) {
                        String relativePath = watchedPath.relativize(path).getName(0).toString();
                        Optional<? extends PathChangeListener> pathChangeListener = listener.processPath(relativePath);
                        if (pathChangeListener.isPresent()) {
                            new PathWatcher(path, rootPath, watchService, pathChangeListener.get()).onRegistered();
                            ;
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
        final void onCreated(SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            try (DirectoryStream<Path> contents = Files.newDirectoryStream(getWatchedPath())) {
                for (Path path : contents) {
                    if (Files.isDirectory(path)) {
                        onDirectoryCreated(path, outChanged);
                    } else {
                        listener.onFileCreated(new DirectoryFileSource.DirectoryFileReference(path.toFile(), rootPath.toFile()), outChanged);
                    }
                }
            } catch (IOException e) {
                logger.error("Error registering path for change watching '{}'", getWatchedPath(), e);
            }
        }
    }

    private class RootPathWatcher implements PathChangeListener {

        private Name module;

        RootPathWatcher(Name module) {
            this.module = module;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) throws IOException {
            switch (target) {
                case ModuleAssetScanner.ASSET_FOLDER: {
                    return Optional.of(new AssetRootPathWatcher(module));
                }
                case ModuleAssetScanner.DELTA_FOLDER: {
                    return Optional.of(new DeltaRootPathWatcher(module));
                }
                case ModuleAssetScanner.OVERRIDE_FOLDER: {
                    return Optional.of(new OverrideRootPathWatcher(module));
                }
            }
            return Optional.empty();
        }
    }

    private class AssetRootPathWatcher implements PathChangeListener {

        private Name module;

        AssetRootPathWatcher(Name module) {
            this.module = module;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) {
            return Optional.of(new AssetPathWatcher(target, module, module));
        }
    }

    private class OverrideRootPathWatcher implements PathChangeListener {

        private Name module;

        OverrideRootPathWatcher(Name module) {
            this.module = module;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) {
            return Optional.of(new OverrideModulePathWatcher(new Name(target), module));
        }
    }

    private class OverrideModulePathWatcher implements PathChangeListener {

        private Name module;
        private Name providingModule;

        OverrideModulePathWatcher(Name module, Name providingModule) {
            this.module = module;
            this.providingModule = providingModule;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) {
            return Optional.of(new AssetPathWatcher(target, module, providingModule));
        }
    }

    private class DeltaRootPathWatcher implements PathChangeListener {

        private Name module;

        DeltaRootPathWatcher(Name module) {
            this.module = module;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) {
            return Optional.of(new DeltaModulePathWatcher(new Name(target), module));
        }
    }

    private class DeltaModulePathWatcher implements PathChangeListener {

        private Name module;
        private Name providingModule;

        DeltaModulePathWatcher(Name module, Name providingModule) {
            this.module = module;
            this.providingModule = providingModule;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) {
            return Optional.of(new DeltaPathWatcher(target, module, providingModule));
        }
    }

    private class DeltaPathWatcher implements PathChangeListener {

        private final Name module;
        private final Name providingModule;
        private final String assetType;


        DeltaPathWatcher(String type, Name module, Name providingModule) {
            this.module = module;
            this.providingModule = providingModule;
            this.assetType = type;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) throws IOException {
            return Optional.of(this);
        }

        @Override
        public void onFileCreated(FileReference file, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Delta added: {}", file);
            notifySubscribers(assetType, file, module, providingModule, FileChangeSubscriber::deltaFileAdded, outChanged);
        }

        @Override
        public void onFileModified(FileReference file, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Delta modified: {}", file);
            notifySubscribers(assetType, file, module, providingModule, FileChangeSubscriber::deltaFileModified, outChanged);
        }

        @Override
        public void onFileDeleted(FileReference file, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Delta deleted: {}", file);
            notifySubscribers(assetType, file, module, providingModule, FileChangeSubscriber::deltaFileDeleted, outChanged);
        }
    }

    private class AssetPathWatcher implements PathChangeListener {

        private String assetType;
        private Name module;
        private Name providingModule;

        AssetPathWatcher(String assetType, Name module, Name providingModule) {
            this.assetType = assetType;
            this.module = module;
            this.providingModule = providingModule;
        }

        @Override
        public Optional<? extends PathChangeListener> processPath(String target) {
            return Optional.of(this);
        }

        @Override
        public void onFileCreated(FileReference target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Asset added: {}", target);
            notifySubscribers(assetType, target, module, providingModule, FileChangeSubscriber::assetFileAdded, outChanged);
        }

        @Override
        public void onFileModified(FileReference target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Asset modified: {}", target);
            notifySubscribers(assetType, target, module, providingModule, FileChangeSubscriber::assetFileModified, outChanged);
        }

        @Override
        public void onFileDeleted(FileReference target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Asset deleted: {}", target);
            notifySubscribers(assetType, target, module, providingModule, FileChangeSubscriber::assetFileDeleted, outChanged);
        }
    }

}
