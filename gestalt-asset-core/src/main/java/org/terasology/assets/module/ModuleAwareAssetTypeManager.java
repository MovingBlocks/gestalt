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

package org.terasology.assets.module;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.AssetFactory;
import org.terasology.assets.AssetType;
import org.terasology.assets.format.AssetAlterationFileFormat;
import org.terasology.assets.format.AssetFileFormat;
import org.terasology.assets.format.producer.AssetFileDataProducer;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.management.AssetTypeManager;
import org.terasology.assets.management.MapAssetTypeManager;
import org.terasology.assets.module.annotations.RegisterAssetDataProducer;
import org.terasology.assets.module.annotations.RegisterAssetDeltaFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetSupplementalFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetType;
import org.terasology.assets.module.autoreload.AssetReloadOnChangeHandler;
import org.terasology.module.ModuleEnvironment;
import org.terasology.util.reflection.ClassFactory;
import org.terasology.util.reflection.GenericsUtil;
import org.terasology.util.reflection.ParameterProvider;
import org.terasology.util.reflection.SimpleClassFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * ModuleAwareAssetTypeManager is an AssetTypeManager that integrates with a ModuleEnvironment, obtaining assets, registering extension classes and handling asset
 * disposal and reloading when environments change.
 * <p>
 * The major features of ModuleAwareAssetTypeManager are:
 * </p>
 * <ul>
 * <li>Registration of core AssetTypes, AssetDataProducers and file formats. These will remain across environment changes.</li>
 * <li>Automatic registration of extension AssetTypes, AssetDataProducers and file formats mark with annotations that are discovered within the module environment
 * being switched to, and removal of these extensions when the module environment is later unloaded</li>
 * <li>Optionally reload all assets from their modules - this is recommended after an environment switch to prevent changes to assets in a previous environment from persisting</li>
 * <li>Optionally enable detection and auto-reload of assets that change on the file system. If this is enabled then {@link #reloadChangedAssets()} must be called regularly to process changes.</li>
 * </ul>
 *
 * @author Immortius
 */
public class ModuleAwareAssetTypeManager implements Closeable, AssetTypeManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleAwareAssetTypeManager.class);

    private final MapAssetTypeManager assetTypeManager = new MapAssetTypeManager();
    private final AssetManager assetManager = new AssetManager(this);
    private final ModuleEnvironmentDependencyProvider dependencyProvider = new ModuleEnvironmentDependencyProvider();
    private final ClassFactory classFactory;
    private final ModuleAssetScanner assetScanner = new ModuleAssetScanner();

    private final Map<AssetType<?, ?>, AssetTypeInfo> assetTypeInfo = Maps.newHashMap();

    private boolean detectingChangedAssets = false;
    private AssetReloadOnChangeHandler reloadOnChangeHandler;

    public ModuleAwareAssetTypeManager() {
        this.classFactory = new SimpleClassFactory(new ParameterProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> get(Class<T> type) {
                if (type.equals(AssetManager.class)) {
                    return (Optional<T>) Optional.of(assetManager);
                }
                return Optional.empty();
            }
        });
    }

    /**
     * @param classFactory The factory to use to instantiate classes for automatic registration.
     */
    public ModuleAwareAssetTypeManager(ClassFactory classFactory) {
        this.classFactory = classFactory;
    }

    @Override
    public synchronized void close() {
        unloadEnvironment();
        assetTypeManager.clear();
        assetTypeInfo.clear();
        assetScanner.clearCache();
    }

    /**
     * @return Whether asset file change detection is enabled
     */
    public boolean isDetectingChangedAssets() {
        return detectingChangedAssets;
    }

    /**
     * Allows enabling or disabling of automatic detection of asset file changes in the module environment. When enabled, all asset types will automatically be hooked up for
     * detection of asset file changes. Calling reloadChangedAssets() will trigger reloading any changed assets.
     *
     * @param detectingChangedAssets Whether to enable or disable detecting asset file changes on the file system
     */
    public synchronized void setDetectingChangedAssets(boolean detectingChangedAssets) {
        this.detectingChangedAssets = detectingChangedAssets;
        if (dependencyProvider.getModuleEnvironment() != null) {
            if (detectingChangedAssets) {
                activateAssetReloader(dependencyProvider.getModuleEnvironment());
            } else {
                deactivateAssetReloader();
            }
        }
    }

    /**
     * Triggers reloading any assets whose files have been changed on the file system
     */
    public synchronized void reloadChangedAssets() {
        if (reloadOnChangeHandler != null) {
            reloadOnChangeHandler.poll();
        }
    }

    private void activateAssetReloader(ModuleEnvironment moduleEnvironment) {
        try {
            reloadOnChangeHandler = new AssetReloadOnChangeHandler(moduleEnvironment);
            for (AssetTypeInfo typeInfo : assetTypeInfo.values()) {
                reloadOnChangeHandler.addAssetType(typeInfo.getAssetType(), typeInfo.getFileProducer());
            }
        } catch (IOException e) {
            logger.error("Failed to activate automatic asset reloading", e);
        }
    }

    private void deactivateAssetReloader() {
        if (reloadOnChangeHandler != null) {
            try {
                reloadOnChangeHandler.close();
            } catch (IOException e) {
                logger.error("Error shutting down AssetReloadOnChangeHandler", e);
            }
            reloadOnChangeHandler = null;
        }
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

    /**
     * Creates and registers an asset type
     *
     * @param type           The type of asset the AssetType will handle
     * @param factory        The factory for creating an asset from asset data
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, String... subfolderNames) {
        return this.createAssetType(type, factory, Arrays.asList(subfolderNames));
    }

    /**
     * Creates and registers an asset type
     *
     * @param type           The type of asset the AssetType will handle
     * @param factory        The factory for creating an asset from asset data
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, Collection<String> subfolderNames) {
        AssetType<T, U> assetType = new AssetType<>(type, factory);
        addAssetType(assetType, subfolderNames);
        return assetType;
    }

    private <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, boolean extension, Collection<String> subfolderNames) {
        AssetType<T, U> assetType = new AssetType<>(type, factory);
        addAssetType(assetType, extension, subfolderNames);
        return assetType;
    }

    /**
     * Registers an asset type
     *
     * @param assetType      The AssetType to register
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, String... subfolderNames) {
        return this.addAssetType(assetType, Arrays.asList(subfolderNames));
    }

    /**
     * Registers an asset type
     *
     * @param assetType      The AssetType to register
     * @param subfolderNames The names of the subfolders providing asset files, if any
     * @param <T>            The type of Asset
     * @param <U>            The type of AssetData
     * @return The new AssetType
     */
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, Collection<String> subfolderNames) {
        return addAssetType(assetType, false, subfolderNames);
    }

    private <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, boolean extension, Collection<String> subfolderNames) {
        assetTypeManager.addAssetType(assetType);
        assetType.setResolutionStrategy(new ModuleDependencyResolutionStrategy(dependencyProvider));
        AssetTypeInfo info = new AssetTypeInfo(assetType, extension);
        AssetFileDataProducer<U> producer = new AssetFileDataProducer<U>(dependencyProvider, subfolderNames);
        info.setFileProducer(producer);
        assetType.addProducer(producer);
        assetTypeInfo.put(assetType, info);
        if (reloadOnChangeHandler != null) {
            reloadOnChangeHandler.addAssetType(assetType, info.getFileProducer());
        }
        return assetType;
    }

    /**
     * @param assetType The AssetType to get the AssetFileDataProducer for. This must be an AssetType handled by this AssetTypeManager
     * @param <T>       The type of Asset handled by the AssetType
     * @param <U>       The type of AssetData handled by the AssetType
     * @return The AssetFileDataProducer for the given AssetType.
     */
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> AssetFileDataProducer<U> getAssetFileDataProducer(AssetType<T, U> assetType) {
        Preconditions.checkArgument(assetTypeInfo.containsKey(assetType));
        return (AssetFileDataProducer<U>) assetTypeInfo.get(assetType).getFileProducer();
    }

    /**
     * Removes and closes an asset type.
     *
     * @param type The type of asset to remove
     * @param <T>  The type of asset
     * @param <U>  The type of asset data
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Asset<U>, U extends AssetData> void removeAssetType(Class<T> type) {
        AssetType<?, ?> assetType = assetTypeManager.removeAssetType(type);
        assetTypeInfo.remove(type);
        assetType.close();
    }

    /**
     * @return An asset manager over this AssetTypeManager.
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * Switches the module environment. This:
     * <ul>
     * <li>Unloads any previously loaded environment</li>
     * <li>Adds any extension AssetTypes, Formats and Producers</li>
     * <li>Registers all the asset files in the new environment</li>
     * <li>Starts detection of asset file changes for the new environment, if {@link #isDetectingChangedAssets()} is true</li>
     * </ul>
     *
     * @param newEnvironment The new module environment
     */
    public synchronized void switchEnvironment(ModuleEnvironment newEnvironment) {
        Preconditions.checkNotNull(newEnvironment);
        unloadEnvironment();

        // Add extensions
        addExtensionAssetTypes(newEnvironment);
        addExtensionFormats(newEnvironment);
        addExtensionProducers(newEnvironment);

        registerAssetFiles(newEnvironment);
        if (detectingChangedAssets) {
            activateAssetReloader(newEnvironment);
        }
    }

    /**
     * Unloads the current module environment, if any. This:
     * <ul>
     * <li>Removes any extension AssetTypes, Formats and Producers</li>
     * <li>Clears all asset files from the old environment</li>
     * <li>Stops detection of asset file changes, if {@link #isDetectingChangedAssets()} is true</li>
     * </ul>
     */
    public synchronized void unloadEnvironment() {
        if (dependencyProvider.getModuleEnvironment() != null) {
            removeExtensionAssetTypes();
            removeExtensionFormats();
            removeExtensionProducers();

            dependencyProvider.setModuleEnvironment(null);
            clearAssetFiles();
            deactivateAssetReloader();
        }
    }

    /**
     * Reloads all assets.
     */
    public void reloadAssets() {
        for (AssetType<?, ?> assetType : assetTypeManager.getAssetTypes()) {
            assetType.getLoadedAssetUrns().forEach(assetType::reload);
        }
    }

    private void registerAssetFiles(ModuleEnvironment newEnvironment) {
        dependencyProvider.setModuleEnvironment(newEnvironment);

        if (detectingChangedAssets) {
            assetScanner.clearCache();
        }

        for (AssetTypeInfo typeInfo : assetTypeInfo.values()) {
            assetScanner.scan(newEnvironment, typeInfo.getFileProducer());
        }
    }

    private void clearAssetFiles() {
        for (AssetTypeInfo info : assetTypeInfo.values()) {
            info.getFileProducer().clearAssetFiles();
        }
    }

    private void removeExtensionProducers() {
        for (AssetTypeInfo info : assetTypeInfo.values()) {
            info.removeAllAssetDataProducers();
        }
    }

    private void removeExtensionFormats() {
        for (AssetTypeInfo info : assetTypeInfo.values()) {
            info.removeAllExtensionFormats();
        }
    }

    private void removeExtensionAssetTypes() {
        for (AssetType<?, ?> assetType : ImmutableList.copyOf(assetTypeManager.getAssetTypes())) {
            AssetTypeInfo info = assetTypeInfo.get(assetType);
            if (info != null && info.isExtension()) {
                assetTypeManager.removeAssetType(assetType.getAssetClass());
                assetTypeInfo.remove(assetType);
            }
        }
    }

    private void addExtensionAssetTypes(ModuleEnvironment environment) {
        for (Class<?> type : environment.getTypesAnnotatedWith(RegisterAssetType.class, input -> Asset.class.isAssignableFrom(input))) {
            Class<? extends Asset> assetClass = (Class<? extends Asset>) type;
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(assetClass, Asset.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register AssetType for '{}' - asset data type must be bound in inheritance tree", assetClass);
                continue;
            }
            RegisterAssetType registrationInfo = assetClass.getAnnotation(RegisterAssetType.class);
            Optional<AssetFactory> factory = classFactory.instantiateClass(registrationInfo.factoryClass());
            if (factory.isPresent()) {
                if (!assetTypeManager.getAssetType(assetClass).isPresent()) {
                    createAssetType(assetClass, factory.get(), true, Arrays.asList(registrationInfo.folderName()));
                } else {
                    logger.error("Asset Type already registered for type '{}' - discarding additional registration", assetClass);
                }
            }
        }
    }

    private void addExtensionFormats(ModuleEnvironment newEnvironment) {
        scanAndRegisterExtension(newEnvironment, AssetFileFormat.class, RegisterAssetFileFormat.class, AssetTypeInfo::addExtensionFileFormat);
        scanAndRegisterExtension(newEnvironment, AssetAlterationFileFormat.class, RegisterAssetSupplementalFileFormat.class, AssetTypeInfo::addExtensionSupplementFormat);
        scanAndRegisterExtension(newEnvironment, AssetAlterationFileFormat.class, RegisterAssetDeltaFileFormat.class, AssetTypeInfo::addExtensionDeltaFormat);
    }

    private void addExtensionProducers(ModuleEnvironment environment) {
        scanAndRegisterExtension(environment, AssetDataProducer.class, RegisterAssetDataProducer.class, AssetTypeInfo::addExtensionProducer);
    }

    private <T> void scanAndRegisterExtension(ModuleEnvironment environment, Class<T> baseType, Class<? extends Annotation> annotationType, BiConsumer<AssetTypeInfo, T> register) {
        ListMultimap<Class<? extends AssetData>, T> extensions = scanForTypes(environment, baseType, annotationType);
        for (Class<? extends AssetData> assetDataClass : extensions.keySet()) {
            for (T extension : extensions.get(assetDataClass)) {
                assetTypeInfo.entrySet().stream()
                        .filter(t -> t.getKey().getAssetDataClass().equals(assetDataClass))
                        .forEach(t -> register.accept(t.getValue(), extension));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ListMultimap<Class<? extends AssetData>, T> scanForTypes(ModuleEnvironment environment, Class<T> baseType, Class<? extends Annotation> annotationType) {
        ListMultimap<Class<? extends AssetData>, T> discoveredTypes = ArrayListMultimap.create();

        for (T format : findAndInstantiateClasses(environment, baseType, annotationType)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(format.getClass(), baseType, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register '{}' - asset data type must be bound in inheritance tree", format.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            discoveredTypes.put(assetDataClass, format);
        }
        return discoveredTypes;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> findAndInstantiateClasses(ModuleEnvironment environment, final Class<T> baseType, Class<? extends Annotation> annotation) {
        List<T> result = Lists.newArrayList();
        for (Class<?> discoveredType : environment.getTypesAnnotatedWith(annotation, input -> baseType.isAssignableFrom(input)
                && !Modifier.isAbstract(input.getModifiers()))) {
            Optional<T> instance = classFactory.instantiateClass((Class<? extends T>) discoveredType);
            instance.ifPresent(result::add);
        }
        return result;
    }

    private static class AssetTypeInfo {
        private boolean extension;
        private AssetType assetType;
        private AssetFileDataProducer fileProducer;
        private List<AssetFileFormat> extensionFileFormats = Lists.newArrayList();
        private List<AssetAlterationFileFormat> extensionSupplementFormats = Lists.newArrayList();
        private List<AssetAlterationFileFormat> extensionDeltaFormats = Lists.newArrayList();
        private List<AssetDataProducer> extensionProducers = Lists.newArrayList();

        AssetTypeInfo(AssetType assetType, boolean extension) {
            this.assetType = assetType;
            this.extension = extension;
        }

        public AssetType getAssetType() {
            return assetType;
        }

        boolean isExtension() {
            return extension;
        }

        AssetFileDataProducer<?> getFileProducer() {
            return fileProducer;
        }

        void setFileProducer(AssetFileDataProducer fileProducer) {
            this.fileProducer = fileProducer;
        }

        @SuppressWarnings("unchecked")
        void addExtensionProducer(AssetDataProducer producer) {
            extensionProducers.add(producer);
            assetType.addProducer(producer);
        }

        @SuppressWarnings("unchecked")
        void addExtensionFileFormat(AssetFileFormat assetFileFormat) {
            extensionFileFormats.add(assetFileFormat);
            fileProducer.addAssetFormat(assetFileFormat);
        }

        @SuppressWarnings("unchecked")
        void addExtensionSupplementFormat(AssetAlterationFileFormat supplementFormat) {
            extensionSupplementFormats.add(supplementFormat);
            fileProducer.addSupplementFormat(supplementFormat);
        }

        @SuppressWarnings("unchecked")
        void addExtensionDeltaFormat(AssetAlterationFileFormat deltaFormat) {
            extensionDeltaFormats.add(deltaFormat);
            fileProducer.addDeltaFormat(deltaFormat);
        }

        @SuppressWarnings("unchecked")
        void removeAllExtensionFormats() {
            extensionFileFormats.forEach(fileProducer::removeAssetFormat);
            extensionFileFormats.clear();
            extensionSupplementFormats.forEach(fileProducer::removeSupplementFormat);
            extensionSupplementFormats.clear();
            extensionDeltaFormats.forEach(fileProducer::removeDeltaFormat);
            extensionDeltaFormats.clear();
        }

        void removeAllAssetDataProducers() {
            extensionProducers.forEach(assetType::removeProducer);
            extensionProducers.clear();
        }
    }
}
