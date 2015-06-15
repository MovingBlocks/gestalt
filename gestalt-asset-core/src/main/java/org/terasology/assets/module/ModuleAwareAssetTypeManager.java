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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.AssetFactory;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResolutionStrategy;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.AssetAlterationFileFormat;
import org.terasology.assets.format.AssetFileFormat;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.management.AssetTypeManager;
import org.terasology.assets.module.annotations.RegisterAssetDataProducer;
import org.terasology.assets.module.annotations.RegisterAssetDeltaFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetSupplementalFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetType;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ModuleAwareAssetTypeManager is an AssetTypeManager that integrates with ModuleEnvironment, obtaining assets, registering extension classes and handling asset
 * disposal and reloading when environments change.
 * <p>
 * The major features of ModuleAwareAssetTypeManager are:
 * </p>
 * <ul>
 * <li>Registration of core AssetTypes, AssetDataProducers and file formats. These will automatically be registered when the environment is next switched,
 * and will remain across environment changes.</li>
 * <li>Automatic registration of extension AssetTypes, AssetDataProducers and file formats mark with annotations that are discovered within the module environment
 * being switched to, and removal of these extensions when the module environment is later switched from</li>
 * <li>When the module environment is changed, all assets are either reloaded if within the new environment, or disposed if no longer available.</li>
 * <li>Will reload assets changed on the file system upon request</li>
 * </ul>
 *
 * @author Immortius
 */
public class ModuleAwareAssetTypeManager implements AssetTypeManager, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ModuleAwareAssetTypeManager.class);

    private final AssetManager assetManager;

    private volatile ImmutableMap<Class<? extends Asset>, AssetType<?, ?>> assetTypes = ImmutableMap.of();
    private volatile ImmutableListMultimap<Class<? extends Asset>, Class<? extends Asset>> subtypes = ImmutableListMultimap.of();

    private final List<AssetType<?, ?>> coreAssetTypes = Lists.newArrayList();
    private final Set<Class<? extends Asset>> reloadOnSwitchAssetTypes = Sets.newHashSet();

    private final SetMultimap<Class<? extends Asset>, String> coreAssetTypeFolderNames = HashMultimap.create();
    private final ListMultimap<Class<? extends Asset<?>>, AssetDataProducer<?>> coreProducers = ArrayListMultimap.create();
    private final ListMultimap<Class<? extends Asset<?>>, AssetFileFormat<?>> coreFormats = ArrayListMultimap.create();
    private final ListMultimap<Class<? extends Asset<?>>, AssetAlterationFileFormat<?>> coreSupplementalFormats = ArrayListMultimap.create();
    private final ListMultimap<Class<? extends Asset<?>>, AssetAlterationFileFormat<?>> coreDeltaFormats = ArrayListMultimap.create();

    private final ClassFactory classFactory;
    private volatile ModuleWatcher watcher;


    public ModuleAwareAssetTypeManager() {
        this.assetManager = new AssetManager(this);
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

    public ModuleAwareAssetTypeManager(ClassFactory classFactory) {
        this.assetManager = new AssetManager(this);
        this.classFactory = classFactory;
    }

    @Override
    public synchronized void close() throws IOException {
        for (AssetType assetType : assetTypes.values()) {
            assetType.close();
        }
        if (watcher != null) {
            watcher.shutdown();
            watcher = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> Optional<AssetType<T, U>> getAssetType(Class<T> type) {
        return Optional.ofNullable((AssetType<T, U>) assetTypes.get(type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<?>> List<AssetType<? extends T, ?>> getAssetTypes(Class<T> type) {
        List<AssetType<? extends T, ?>> result = Lists.newArrayList();
        for (Class<? extends Asset> subtype : subtypes.get(type)) {
            AssetType<? extends T, ?> subAssetType = (AssetType<? extends T, ?>) assetTypes.get(subtype);
            if (subAssetType != null) {
                result.add(subAssetType);
            }
        }
        return result;
    }

    /**
     * Triggers the reload of any assets that have been altered in directory modules.
     */
    public void reloadChangedOnDisk() {
        if (watcher != null) {
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
    }

    /**
     * Registers an asset type. It will be available after the next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called. Asset files will be
     * read from modules from the provided subfolders. If there are no subfolders then assets will not be loaded from modules.
     *
     * @param type           The type of to register as a core type
     * @param factory        The factory to create assets of the desired type from asset data
     * @param subfolderNames The name of the subfolders which asset files related to this type will be read from within modules
     * @param <T>            The type of asset
     * @param <U>            The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void registerCoreAssetType(Class<T> type, AssetFactory<T, U> factory, String... subfolderNames) {
        registerCoreAssetType(type, factory, true, subfolderNames);
    }

    /**
     * Registers an asset type. It will be available after the next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called. Asset files will be
     * read from modules from the provided subfolders. If there are no subfolders then assets will not be loaded from modules.
     *
     * @param type           The type of to register as a core type
     * @param factory        The factory to create assets of the desired type from asset data
     * @param reloadOnSwitch Whether assets of this type should be reloaded on environment switch rather than just disposed
     * @param subfolderNames The name of the subfolders which asset files related to this type will be read from within modules
     * @param <T>            The type of asset
     * @param <U>            The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void registerCoreAssetType(Class<T> type, AssetFactory<T, U> factory, boolean reloadOnSwitch,
                                                                                             String... subfolderNames) {
        Preconditions.checkState(!assetTypes.containsKey(type), "Asset type '" + type.getSimpleName() + "' already registered");
        AssetType<T, U> assetType = new AssetType<>(type, factory);
        coreAssetTypes.add(assetType);
        coreAssetTypeFolderNames.putAll(type, Arrays.asList(subfolderNames));
        if (reloadOnSwitch) {
            reloadOnSwitchAssetTypes.add(type);
        }
    }

    /**
     * Removes an asset type. This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param type The type of asset to remove
     * @param <T>  The type of asset
     * @param <U>  The type of asset data
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Asset<U>, U extends AssetData> void removeCoreAssetType(Class<T> type) {
        Iterator<AssetType<?, ?>> iterator = coreAssetTypes.iterator();
        while (iterator.hasNext()) {
            AssetType<?, ?> assetType = iterator.next();
            if (assetType.getAssetClass() == type) {
                iterator.remove();
                coreAssetTypeFolderNames.removeAll(type);
                reloadOnSwitchAssetTypes.remove(type);
                break;
            }
        }
    }

    /**
     * Registers an AssetDataProducer for use by a specified asset type. This change will take affect next time
     * {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset the producer should be registered with
     * @param producer  The AssetDataProducer.
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void registerCoreProducer(Class<T> assetType, AssetDataProducer<U> producer) {
        coreProducers.put(assetType, producer);
    }

    /**
     * Removes an AssetDataProducer. This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset the producer was registered with
     * @param producer  The AssetDataProducer
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void removeCoreProducer(Class<T> assetType, AssetDataProducer<U> producer) {
        coreProducers.remove(assetType, producer);
    }

    /**
     * Registers an asset file format with a specific asset type.
     * This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset to register the format with.
     * @param format    The AssetFileFormat
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void registerCoreFormat(Class<T> assetType, AssetFileFormat<U> format) {
        coreFormats.put(assetType, format);
    }

    /**
     * Removes an asset file format. This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset to remove the format from.
     * @param format    The AssetFileFormat
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void removeCoreFormat(Class<T> assetType, AssetFileFormat<U> format) {
        coreFormats.remove(assetType, format);
    }

    /**
     * Registers an asset supplemental format with a specific asset type.
     * This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset to register the format with.
     * @param format    The supplemental file format
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void registerCoreSupplementalFormat(Class<T> assetType, AssetAlterationFileFormat<U> format) {
        coreSupplementalFormats.put(assetType, format);
    }

    /**
     * Removes an asset supplemental format. This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset to remove the format from.
     * @param format    The supplemental file format.
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void removeCoreSupplementalFormat(Class<T> assetType, AssetAlterationFileFormat<U> format) {
        coreSupplementalFormats.remove(assetType, format);
    }

    /**
     * Registers an asset delta format with a specific asset type.
     * This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset to register the format with.
     * @param format    The delta file format.
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void registerCoreDeltaFormat(Class<T> assetType, AssetAlterationFileFormat<U> format) {
        coreDeltaFormats.put(assetType, format);
    }

    /**
     * Removes an asset delta format.
     * This change will take affect next time {@link #switchEnvironment(org.terasology.module.ModuleEnvironment)} is called.
     *
     * @param assetType The type of asset to remove the format from
     * @param format    The delta file format.
     * @param <T>       The type of asset
     * @param <U>       The type of asset data
     */
    public synchronized <T extends Asset<U>, U extends AssetData> void removeCoreDeltaFormat(Class<T> assetType, AssetAlterationFileFormat<U> format) {
        coreDeltaFormats.remove(assetType, format);
    }

    /**
     * @return An asset manager over this AssetTypeManager.
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * Switches the module environment. This triggers:
     * <ul>
     * <li>Removal of all extension types, producers and formats</li>
     * <li>Disposal of all assets not present in the new environment</li>
     * <li>Reload of all assets present in the new environment</li>
     * <li>Scan for and install extension asset types, producers and formats</li>
     * <li>Makes available loading assets from the new environment</li>
     * </ul>
     *
     * @param newEnvironment The new module environment
     */
    public synchronized void switchEnvironment(ModuleEnvironment newEnvironment) {
        Preconditions.checkNotNull(newEnvironment);

        if (watcher != null) {
            try {
                watcher.shutdown();
                watcher = null;
            } catch (IOException e) {
                logger.error("Failed to shut down watch service", e);
            }
        }

        try {
            watcher = new ModuleWatcher(newEnvironment);
        } catch (IOException e) {
            logger.warn("Failed to establish watch service, will not auto-reload changed assets", e);
        }

        for (AssetType<?, ?> assetType : assetTypes.values()) {
            assetType.clearProducers();
        }

        ListMultimap<Class<? extends AssetData>, AssetFileFormat<?>> extensionFileFormats = scanForExtensionFormats(newEnvironment);
        ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionSupplementalFormats = scanForExtensionSupplementalFormats(newEnvironment);
        ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionDeltaFormats = scanForExtensionDeltaFormats(newEnvironment);
        ResolutionStrategy resolutionStrategy = new ModuleDependencyResolutionStrategy(newEnvironment);

        Map<Class<? extends Asset>, AssetType<?, ?>> newAssetTypes = Maps.newHashMap();

        setupCoreAssetTypes(newEnvironment, extensionFileFormats, extensionSupplementalFormats, extensionDeltaFormats, resolutionStrategy, newAssetTypes);
        setupExtensionAssetTypes(newEnvironment, extensionFileFormats, extensionSupplementalFormats, extensionDeltaFormats, resolutionStrategy, newAssetTypes);
        scanForExtensionProducers(newEnvironment, newAssetTypes);

        ImmutableMap<Class<? extends Asset>, AssetType<?, ?>> oldAssetTypes = assetTypes;
        assetTypes = ImmutableMap.copyOf(newAssetTypes);
        for (AssetType<?, ?> assetType : assetTypes.values()) {
            if (reloadOnSwitchAssetTypes.contains(assetType.getAssetClass())) {
                assetType.refresh();
            } else {
                assetType.disposeAll();
            }
        }

        oldAssetTypes.values().stream().filter(assetType -> !coreAssetTypes.contains(assetType)).forEach(assetType -> assetType.close());
        updateSubtypesMap();
    }

    /**
     * Updates the map of subtypes based on the current asset types
     */
    private void updateSubtypesMap() {
        ListMultimap<Class<? extends Asset>, Class<? extends Asset>> subtypesBuilder = ArrayListMultimap.create();
        for (Class<? extends Asset> type : assetTypes.keySet()) {
            for (Class<?> parentType : ReflectionUtils.getAllSuperTypes(type, new Predicate<Class<?>>() {
                @Override
                public boolean apply(Class<?> input) {
                    return Asset.class.isAssignableFrom(input) && input != Asset.class;
                }
            })) {
                subtypesBuilder.put((Class<? extends Asset>) parentType, type);
                Collections.sort(subtypesBuilder.get((Class<? extends Asset>) parentType), new Comparator<Class<?>>() {
                    @Override
                    public int compare(Class<?> o1, Class<?> o2) {
                        return o1.getSimpleName().compareTo(o2.getSimpleName());
                    }
                });
            }
        }
        subtypes = ImmutableListMultimap.copyOf(subtypesBuilder);
    }

    private void subscribeToChanges(AssetType<?, ?> assetType, ModuleAssetDataProducer<?> producer, Collection<String> folderNames) {
        if (watcher != null) {
            for (String folder : folderNames) {
                watcher.register(folder, producer, assetType);
            }
        }
    }

    private void setupCoreAssetTypes(ModuleEnvironment environment, ListMultimap<Class<? extends AssetData>, AssetFileFormat<?>> extensionFileFormats,
                                     ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionSupplementalFormats,
                                     ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionDeltaFormats, ResolutionStrategy resolutionStrategy,
                                     Map<Class<? extends Asset>, AssetType<?, ?>> outAssetTypes) {
        for (AssetType<?, ?> assetType : coreAssetTypes) {
            Set<String> folderNames = coreAssetTypeFolderNames.get(assetType.getAssetClass());
            prepareAssetType(assetType, folderNames, resolutionStrategy, environment, extensionFileFormats, extensionSupplementalFormats, extensionDeltaFormats);
            outAssetTypes.put(assetType.getAssetClass(), assetType);
        }
    }

    private void setupExtensionAssetTypes(ModuleEnvironment environment, ListMultimap<Class<? extends AssetData>, AssetFileFormat<?>> extensionFileFormats,
                                          ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionSupplementalFormats,
                                          ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionDeltaFormats,
                                          ResolutionStrategy resolutionStrategy, Map<Class<? extends Asset>, AssetType<?, ?>> outAssetTypes) {
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
                AssetType<?, ?> assetType = new AssetType<>(assetClass, factory.get());
                prepareAssetType(assetType, Arrays.asList(registrationInfo.folderName()), resolutionStrategy, environment,
                        extensionFileFormats, extensionSupplementalFormats, extensionDeltaFormats);
                if (!outAssetTypes.containsKey(assetType.getAssetClass())) {
                    outAssetTypes.put(assetType.getAssetClass(), assetType);
                } else {
                    logger.error("Asset Type already registered for type '{}' - discarding additional registration", assetType.getAssetClass());
                    assetType.close();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Asset<U>, U extends AssetData> void prepareAssetType(
            AssetType<T, U> assetType,
            Collection<String> folderNames,
            ResolutionStrategy resolutionStrategy,
            ModuleEnvironment environment,
            ListMultimap<Class<? extends AssetData>, AssetFileFormat<?>> extensionFileFormats,
            ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionSupplementalFormats,
            ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionDeltaFormats) {
        assetType.setResolutionStrategy(resolutionStrategy);
        for (AssetDataProducer producer : coreProducers.get(assetType.getAssetClass())) {
            assetType.addProducer(producer);
        }

        if (!folderNames.isEmpty()) {
            List<AssetFileFormat<?>> assetFormats = Lists.newArrayList(coreFormats.get(assetType.getAssetClass()));
            assetFormats.addAll(extensionFileFormats.get(assetType.getAssetDataClass()));

            List<AssetAlterationFileFormat<?>> supplementalFormats = Lists.newArrayList(coreSupplementalFormats.get(assetType.getAssetClass()));
            supplementalFormats.addAll(extensionSupplementalFormats.get(assetType.getAssetDataClass()));

            List<AssetAlterationFileFormat<?>> deltaFormats = Lists.newArrayList(coreDeltaFormats.get(assetType.getAssetClass()));
            deltaFormats.addAll(extensionDeltaFormats.get(assetType.getAssetDataClass()));

            ModuleAssetDataProducer moduleProducer = new ModuleAssetDataProducer(environment, assetFormats, supplementalFormats, deltaFormats, folderNames);
            assetType.addProducer(moduleProducer);
            subscribeToChanges(assetType, moduleProducer, folderNames);
        }
    }

    @SuppressWarnings("unchecked")
    private void scanForExtensionProducers(ModuleEnvironment environment, Map<Class<? extends Asset>, AssetType<?, ?>> forAssetTypes) {
        for (AssetDataProducer producer : findAndInstantiateClasses(environment, AssetDataProducer.class, RegisterAssetDataProducer.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(producer.getClass(), AssetDataProducer.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register AssetProducer '{}' - asset data type must be bound in inheritance tree", producer.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            List<AssetType<?, ?>> validAssetTypes = Lists.newArrayList(Collections2.filter(forAssetTypes.values(), new Predicate<AssetType<?, ?>>() {
                @Override
                public boolean apply(AssetType<?, ?> input) {
                    return input.getAssetDataClass().equals(assetDataClass);
                }
            }));
            if (validAssetTypes.isEmpty()) {
                logger.error("No asset type available for asset producer {}", producer.getClass());
            } else {
                for (AssetType<?, ?> assetType : validAssetTypes) {
                    assetType.addProducer(producer);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ListMultimap<Class<? extends AssetData>, AssetFileFormat<?>> scanForExtensionFormats(ModuleEnvironment environment) {
        ListMultimap<Class<? extends AssetData>, AssetFileFormat<?>> extensionFormats = ArrayListMultimap.create();

        for (AssetFileFormat format : findAndInstantiateClasses(environment, AssetFileFormat.class, RegisterAssetFileFormat.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(format.getClass(), AssetFileFormat.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register AssetFormat '{}' - asset data type must be bound in inheritance tree", format.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            extensionFormats.put(assetDataClass, format);
        }
        return extensionFormats;
    }

    @SuppressWarnings("unchecked")
    private ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> scanForExtensionSupplementalFormats(ModuleEnvironment environment) {
        ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionFormats = ArrayListMultimap.create();

        for (AssetAlterationFileFormat format : findAndInstantiateClasses(environment, AssetAlterationFileFormat.class, RegisterAssetSupplementalFileFormat.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(format.getClass(), AssetAlterationFileFormat.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register Asset Supplemental Format '{}' - asset data type must be bound in inheritance tree", format.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            extensionFormats.put(assetDataClass, format);
        }
        return extensionFormats;
    }

    @SuppressWarnings("unchecked")
    private ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> scanForExtensionDeltaFormats(ModuleEnvironment environment) {
        ListMultimap<Class<? extends AssetData>, AssetAlterationFileFormat<?>> extensionFormats = ArrayListMultimap.create();

        for (AssetAlterationFileFormat format : findAndInstantiateClasses(environment, AssetAlterationFileFormat.class, RegisterAssetDeltaFileFormat.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(format.getClass(), AssetAlterationFileFormat.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register Asset Delta Format '{}' - asset data type must be bound in inheritance tree", format.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            extensionFormats.put(assetDataClass, format);
        }
        return extensionFormats;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> findAndInstantiateClasses(ModuleEnvironment environment, final Class<T> baseType, Class<? extends Annotation> annotation) {
        List<T> result = Lists.newArrayList();
        for (Class<?> discoveredType : environment.getTypesAnnotatedWith(annotation, input -> baseType.isAssignableFrom(input)
                && !Modifier.isAbstract(input.getModifiers()))) {
            Optional<T> instance = classFactory.instantiateClass((Class<? extends T>) discoveredType);
            if (instance.isPresent()) {
                result.add(instance.get());
            }
        }
        return result;
    }

}
