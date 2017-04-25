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
import com.google.common.collect.SetMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.producer.AssetFileDataProducer;
import org.terasology.module.ModuleEnvironment;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AssetReloadOnChangeHandler sets up a watcher over asset files in a module environment. When these files change the relevant ModuleAssetDataProducer is notified and the
 * asset is reloaded. The poll method must be called to process file system change event and reload assets.
 * <p>AssetReloadOnChangeHandler should be closed when no longer needed - such as when the module environment is being shut down - so that its file system handles can
 * be cleaned up</p>
 */
public class AssetReloadOnChangeHandler implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AssetReloadOnChangeHandler.class);

    private final ModuleEnvironmentWatcher watcher;

    /**
     * @param environment The module environment to monitor for changes
     * @throws IOException If an error prevents reload support from being set up
     */
    public AssetReloadOnChangeHandler(ModuleEnvironment environment) throws IOException {
        watcher = new ModuleEnvironmentWatcher(environment);
    }

    /**
     * Registers a AssetType/AssetFileDataProducer pair to be notified of changes on the file system
     * @param assetType The AssetType
     * @param producer The AssetFileDataProducer to notify of changes
     */
    public void addAssetType(AssetType<?, ?> assetType, AssetFileDataProducer<?> producer) {
        for (String folder : producer.getFolderNames()) {
            watcher.register(folder, producer, assetType);
        }
    }

    /**
     * Unregisters an asset type and all its producers from file change notifications
     * @param assetType The AssetType to unregister
     */
    public void removeAssetType(AssetType<?, ?> assetType) {
        watcher.unregister(assetType);
    }

    /**
     * Processes and change events and reloads modified assets.
     * @throws IllegalStateException if the AssetReloadOnChangeHandler has been closed.
     */
    public void poll() {
        Preconditions.checkState(!watcher.isClosed(), "AutoReloadOnChangeManager has been closed");
        SetMultimap<AssetType<?, ?>, ResourceUrn> changes = watcher.checkForChanges();
        for (Map.Entry<AssetType<?, ?>, ResourceUrn> entry : changes.entries()) {
            if (entry.getKey().isLoaded(entry.getValue())) {
                AssetType<?, ?> assetType = entry.getKey();
                ResourceUrn changedUrn = entry.getValue();
                logger.info("Reloading changed asset '{}'", changedUrn);
                assetType.reload(changedUrn);
            }
        }
    }

    @Override
    public void close() throws IOException {
        watcher.shutdown();
    }
}
