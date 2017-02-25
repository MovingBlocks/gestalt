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

package org.terasology.assets.module.autoreload;

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
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.module.ModuleAssetDataProducer;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;

/**
 * Centralised detector of changes to asset files (creation, modification, deletion), to inform ModuleAssetDataProducers of the changes
 * and trigger the reload of those assets.
 * <p>
 * One small note: Sometimes it is possible to get file change notifications before the change has actually happened. I observed this under windows
 * when using the New->Folder/New->File optional in explorer - the path will report as not existing unless you wait a short period of time. To address
 * this those events are added into a unreadyEvents queue and processed next check.
 * </p>
 *
 * @author Immortius
 */
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
    public ModuleEnvironmentWatcher(ModuleEnvironment environment) throws IOException {
        this.service = environment.getFileSystem().newWatchService();

        for (Path rootPath : environment.getFileSystem().getRootDirectories()) {
            try {
                Module module = environment.get(new Name(rootPath.getName(0).toString()));
                boolean canWatch = module.getLocations().stream().anyMatch(location -> FileSystems.getDefault().equals(location.getFileSystem()));
                if (canWatch) {
                    PathWatcher watcher = new RootPathWatcher(rootPath, module.getId(), service);
                    watcher.onRegistered();
                }
            } catch (IOException e) {
                logger.warn("Failed to establish change watch service for path '{}'", rootPath, e);
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
    public synchronized void register(String folderName, AssetFileChangeSubscriber subscriber, AssetType<?, ?> assetType) {
        Preconditions.checkState(!closed, "Cannot register folder into closed ModuleWatcher");
        subscribers.put(folderName, new SubscriberInfo(assetType, subscriber));
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Shuts down the ModuleWatcher.
     *
     * @throws IOException If there is an error shutting down the service
     */
    public synchronized void shutdown() throws IOException {
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
    public synchronized SetMultimap<AssetType<?, ?>, ResourceUrn> checkForChanges() {
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
            changed.putAll(pathWatcher.update(key.pollEvents(), Optional.of(unreadyEvents)));
            key.reset();
            key = service.poll();
        }
        return changed;
    }

    /**
     * Notifies subscribers of an asset file change
     *
     * @param folderName      The asset folder in which the changed asset resides
     * @param target          The path of the file
     * @param module          The module the file contributes to
     * @param providingModule The module that provides the file
     * @param method          The subscription method to call to notify
     * @param outChanged      A map of asset types and their changed urns to add any modified resource urns to.
     */
    private void notifySubscribers(String folderName, Path target, Name module, Name providingModule, SubscriptionMethod method,
                                   SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
        for (SubscriberInfo subscriber : subscribers.get(folderName)) {

            Optional<ResourceUrn> urn = method.notify(subscriber.subscriber, target, module, providingModule);
            if (urn.isPresent()) {
                outChanged.put(subscriber.type, urn.get());
            }
        }
    }

    /**
     * Information on a subscriber.
     */
    private static class SubscriberInfo {
        public final AssetType<?, ?> type;
        public final AssetFileChangeSubscriber subscriber;

        public SubscriberInfo(AssetType<?, ?> type, AssetFileChangeSubscriber subscriber) {
            this.type = type;
            this.subscriber = subscriber;
        }
    }

    /**
     * The form of a method to call to notify a subscriber of changes
     */
    private interface SubscriptionMethod {
        Optional<ResourceUrn> notify(AssetFileChangeSubscriber subscriber, Path path, Name module, Name providingModule);
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
        public final SetMultimap<AssetType<?, ?>, ResourceUrn> update(List<WatchEvent<?>> watchEvents, Optional<Collection<DelayedEvent>> outDelayedEvents) {
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
                        onFileCreated(target, changedAssets);
                    } else if (outDelayedEvents.isPresent()) {
                        outDelayedEvents.get().add(new DelayedEvent(event, this));
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

        private void onDirectoryCreated(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
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
        public final void onCreated(SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
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
        protected void onFileCreated(Path target, final SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
        }

        /**
         * Called when a file is modified
         *
         * @param target     The modified file
         * @param outChanged The ResourceUrns of any assets affected
         */
        protected void onFileModified(Path target, final SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
        }

        /**
         * Called when a file is deleted
         *
         * @param target     The deleted file
         * @param outChanged The ResourceUrns of any assets affected
         */
        protected void onFileDeleted(Path target, final SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
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
            if (target.getNameCount() == 2) {
                switch (target.getName(1).toString()) {
                    case ModuleAssetDataProducer.ASSET_FOLDER: {
                        return Optional.of(new AssetRootPathWatcher(target, module, getWatchService()));
                    }
                    case ModuleAssetDataProducer.DELTA_FOLDER: {
                        return Optional.of(new DeltaRootPathWatcher(target, module, getWatchService()));
                    }
                    case ModuleAssetDataProducer.OVERRIDE_FOLDER: {
                        return Optional.of(new OverrideRootPathWatcher(target, module, getWatchService()));
                    }
                }
            }
            return Optional.empty();
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
            if (target.getNameCount() == 3) {
                return Optional.of(new AssetPathWatcher(target, target.getName(2).toString(), module, module, getWatchService()));
            }
            return Optional.empty();
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
            if (target.getNameCount() == 3) {
                return Optional.of(new OverrideRootPathWatcher(target, module, getWatchService()));
            } else if (target.getNameCount() == 4) {
                return Optional.of(new AssetPathWatcher(target, target.getName(3).toString(), new Name(target.getName(2).toString()), module, getWatchService()));
            }
            return Optional.empty();
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
            if (target.getNameCount() == 3) {
                return Optional.of(new DeltaRootPathWatcher(target, module, getWatchService()));
            } else if (target.getNameCount() == 4) {
                return Optional.of(new DeltaPathWatcher(target, new Name(target.getName(2).toString()), module, getWatchService()));
            }
            return Optional.empty();
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

        @Override
        protected void onFileCreated(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Delta added: {}", target);
            String folderName = target.getName(3).toString();
            notifySubscribers(folderName, target, module, providingModule, AssetFileChangeSubscriber::deltaFileAdded, outChanged);
        }

        @Override
        protected void onFileModified(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Delta modified: {}", target);
            String folderName = target.getName(3).toString();
            notifySubscribers(folderName, target, module, providingModule, AssetFileChangeSubscriber::deltaFileModified, outChanged);
        }

        @Override
        protected void onFileDeleted(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Delta deleted: {}", target);
            String folderName = target.getName(3).toString();
            notifySubscribers(folderName, target, module, providingModule, AssetFileChangeSubscriber::deltaFileDeleted, outChanged);
        }
    }

    private class AssetPathWatcher extends PathWatcher {

        private String folderName;
        private Name module;
        private Name providingModule;

        public AssetPathWatcher(Path path, String folderName, Name module, Name providingModule, WatchService watchService) throws IOException {
            super(path, watchService);
            this.folderName = folderName;
            this.module = module;
            this.providingModule = providingModule;
        }

        @Override
        protected Optional<? extends PathWatcher> processPath(Path target) throws IOException {
            return Optional.of(new AssetPathWatcher(target, folderName, module, providingModule, getWatchService()));
        }

        @Override
        protected void onFileCreated(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Asset added: {}", target);
            notifySubscribers(folderName, target, module, providingModule, AssetFileChangeSubscriber::assetFileAdded, outChanged);
        }

        @Override
        protected void onFileModified(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Asset modified: {}", target);
            notifySubscribers(folderName, target, module, providingModule, AssetFileChangeSubscriber::assetFileModified, outChanged);
        }

        @Override
        protected void onFileDeleted(Path target, SetMultimap<AssetType<?, ?>, ResourceUrn> outChanged) {
            logger.debug("Asset deleted: {}", target);
            notifySubscribers(folderName, target, module, providingModule, AssetFileChangeSubscriber::assetFileDeleted, outChanged);
        }
    }

    private static class DelayedEvent {
        private WatchEvent<?> event;
        private PathWatcher watcher;

        public DelayedEvent(WatchEvent<?> event, PathWatcher watcher) {
            this.event = event;
            this.watcher = watcher;
        }

        public SetMultimap<AssetType<?, ?>, ResourceUrn> replay() {
            return watcher.update(Arrays.asList(event), Optional.empty());
        }
    }

}
