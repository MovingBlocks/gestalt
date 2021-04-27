// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.assets.module.autoreload;

import android.support.annotation.RequiresApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.assets.Asset;
import org.terasology.gestalt.assets.AssetData;
import org.terasology.gestalt.assets.AssetFactory;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.format.producer.AssetFileDataProducer;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.gestalt.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.gestalt.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.util.reflection.ClassFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper around ModuleAwareAssetTypeManager that integrates auto asset reloading
 */
@RequiresApi(26)
public class AutoReloadAssetTypeManager implements ModuleAwareAssetTypeManager {

    private static final Logger logger = LoggerFactory.getLogger(AutoReloadAssetTypeManager.class);

    private final ModuleAwareAssetTypeManager assetTypeManager;
    private AssetReloadOnChangeHandler reloadOnChangeHandler;

    public AutoReloadAssetTypeManager() {
        this.assetTypeManager = new ModuleAwareAssetTypeManagerImpl();
    }

    /**
     * @param classFactory The factory to use to instantiate classes for automatic registration.
     */
    public AutoReloadAssetTypeManager(ClassFactory classFactory) {
        this.assetTypeManager = new ModuleAwareAssetTypeManagerImpl(classFactory);
    }

    public AutoReloadAssetTypeManager(ModuleAwareAssetTypeManager assetTypeManager) {
        this.assetTypeManager = assetTypeManager;
    }

    /**
     * Reloads all assets that have changed on disk
     */
    public synchronized void reloadChangedAssets() {
        if (reloadOnChangeHandler != null) {
            reloadOnChangeHandler.poll();
        }
    }

    private synchronized <U extends AssetData, T extends Asset<U>> void registerAssetType(AssetType<T, U> assetType) {
        if (reloadOnChangeHandler != null) {
            reloadOnChangeHandler.addAssetType(assetType, assetTypeManager.getAssetFileDataProducer(assetType));
        }
    }

    private synchronized void openReloadOnChangeHandler(ModuleEnvironment newEnvironment) {
        try {
            reloadOnChangeHandler = new AssetReloadOnChangeHandler(newEnvironment);
            for (AssetType<?, ?> assetType : getAssetTypes()) {
                reloadOnChangeHandler.addAssetType(assetType, getAssetFileDataProducer(assetType));
            }
        } catch (IOException e) {
            logger.error("Failed to instantiate asset reload on change handler", e);
        }
    }

    private synchronized void closeReloadOnChangeHandler() {
        if (reloadOnChangeHandler != null) {
            try {
                reloadOnChangeHandler.close();
            } catch (IOException e) {
                logger.error("Failed to close asset reload on change handler", e);
            }
        }
    }


    @Override
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, String... subfolderNames) {
        AssetType<T, U> assetType = assetTypeManager.createAssetType(type, factory, subfolderNames);
        registerAssetType(assetType);
        return assetType;
    }


    @Override
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, Collection<String> subfolderNames) {
        AssetType<T, U> assetType = assetTypeManager.createAssetType(type, factory, subfolderNames);
        registerAssetType(assetType);
        return assetType;
    }

    @Override
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, String... subfolderNames) {
        assetTypeManager.addAssetType(assetType, subfolderNames);
        registerAssetType(assetType);
        return assetType;
    }

    @Override
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, Collection<String> subfolderNames) {
        assetTypeManager.addAssetType(assetType, subfolderNames);
        registerAssetType(assetType);
        return assetType;
    }

    @Override
    public <T extends Asset<U>, U extends AssetData> AssetFileDataProducer<U> getAssetFileDataProducer(AssetType<T, U> assetType) {
        return assetTypeManager.getAssetFileDataProducer(assetType);
    }

    @Override
    public <T extends Asset<U>, U extends AssetData> void removeAssetType(Class<T> type) {
        assetTypeManager.getAssetType(type).ifPresent(
                assetType -> {
                    assetTypeManager.removeAssetType(type);
                    synchronized (this) {
                        if (reloadOnChangeHandler != null) {
                            reloadOnChangeHandler.removeAssetType(assetType);
                        }
                    }
                }
        );
    }

    @Override
    public AssetManager getAssetManager() {
        return assetTypeManager.getAssetManager();
    }

    @Override
    public synchronized void switchEnvironment(ModuleEnvironment newEnvironment) {
        closeReloadOnChangeHandler();
        assetTypeManager.clearAvailableAssetCache();
        assetTypeManager.switchEnvironment(newEnvironment);
        openReloadOnChangeHandler(newEnvironment);
    }

    @Override
    public void unloadEnvironment() {
        closeReloadOnChangeHandler();
        assetTypeManager.unloadEnvironment();
    }

    @Override
    public void reloadAssets() {
        assetTypeManager.reloadAssets();
    }

    @Override
    public void clearAvailableAssetCache() {
        assetTypeManager.clearAvailableAssetCache();
    }

    @Override
    public void close() throws IOException {
        closeReloadOnChangeHandler();
        assetTypeManager.close();
    }

    @Override
    public <T extends Asset<U>, U extends AssetData> Optional<AssetType<T, U>> getAssetType(Class<T> type) {
        return assetTypeManager.getAssetType(type);
    }

    @Override
    public <T extends Asset<?>> List<AssetType<? extends T, ?>> getAssetTypes(Class<T> type) {
        return assetTypeManager.getAssetTypes(type);
    }

    @Override
    public Collection<AssetType<?, ?>> getAssetTypes() {
        return assetTypeManager.getAssetTypes();
    }

    @Override
    public void disposedUnusedAssets() {
        assetTypeManager.disposedUnusedAssets();
    }

}
