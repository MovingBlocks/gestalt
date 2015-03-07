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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.Asset;
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetFactory;
import org.terasology.assets.module.annotations.RegisterAssetDeltaFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetDataProducer;
import org.terasology.assets.module.annotations.RegisterAssetSupplementalFileFormat;
import org.terasology.assets.module.annotations.RegisterAssetType;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.AssetType;
import org.terasology.assets.management.AssetTypeManager;
import org.terasology.assets.format.AssetAlterationFileFormat;
import org.terasology.assets.format.AssetFileFormat;
import org.terasology.module.ModuleEnvironment;
import org.terasology.assets.ResourceUrn;
import org.terasology.util.reflection.GenericsUtil;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Immortius
 */
public class ModuleAwareAssetTypeManager implements AssetTypeManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleAwareAssetTypeManager.class);

    private final AssetManager assetManager;

    private Map<Class<? extends Asset>, AssetType<?, ?>> assetTypes = Maps.newLinkedHashMap();
    private ListMultimap<Class<? extends Asset>, Class<? extends Asset>> subtypes = ArrayListMultimap.create();
    private Map<Class<? extends Asset>, ModuleAssetDataProducer<?>> moduleProducers = Maps.newLinkedHashMap();

    private List<AssetType<?, ?>> extensionAssetTypes = Lists.newArrayList();
    private ListMultimap<AssetDataProducer<?>, AssetType<?, ?>> extensionProducers = ArrayListMultimap.create();
    private ListMultimap<AssetFileFormat<?>, AssetType<?, ?>> extensionFormats = ArrayListMultimap.create();
    private ListMultimap<AssetAlterationFileFormat<?>, AssetType<?, ?>> extensionSupplementalFormats = ArrayListMultimap.create();
    private ListMultimap<AssetAlterationFileFormat<?>, AssetType<?, ?>> extensionDeltaFormats = ArrayListMultimap.create();

    private ModuleEnvironment environment;

    public ModuleAwareAssetTypeManager(ModuleEnvironment environment) {
        Preconditions.checkNotNull(environment);
        this.environment = environment;
        this.assetManager = new AssetManager(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> getAssetType(Class<T> type) {
        return (AssetType<T, U>) assetTypes.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> List<AssetType<? extends T, ? extends U>> getAssetTypes(Class<T> type) {
        List<AssetType<? extends T, ? extends U>> result = Lists.newArrayList();
        for (Class<? extends Asset> subtype : subtypes.get(type)) {
            result.add((AssetType<? extends T, ? extends U>) assetTypes.get(subtype));
        }
        return result;
    }

    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> registerCoreAssetType(Class<T> type, AssetFactory<T, U> factory) {
        Preconditions.checkState(!assetTypes.containsKey(type), "Asset type '" + type.getSimpleName() + "' already registered");
        AssetType<T, U> assetType = new AssetType<>(type);
        assetType.setFactory(factory);
        assetTypes.put(type, assetType);
        addSubtypesFor(type);
        return assetType;
    }

    public <T extends Asset<U>, U extends AssetData> AssetType<T, U> registerCoreAssetType(Class<T> type, AssetFactory<T, U> factory, String folderName) {
        AssetType<T, U> assetType = registerCoreAssetType(type, factory);
        ModuleAssetDataProducer<U> moduleProducer = new ModuleAssetDataProducer<>(folderName);
        moduleProducer.setEnvironment(environment);
        assetType.addProducer(moduleProducer);
        moduleProducers.put(type, moduleProducer);
        return assetType;
    }

    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> void removeCoreAssetType(Class<T> type) {
        AssetType<T, U> assetType = (AssetType<T, U>) assetTypes.remove(type);
        if (assetType != null) {
            removeSubtypesFor(type);
            assetType.close();
            moduleProducers.remove(type);
        }
    }

    @SuppressWarnings("unchecked")
    private void addSubtypesFor(Class<? extends Asset> type) {
        for (Class<?> parentType : ReflectionUtils.getAllSuperTypes(type, new Predicate<Class<?>>() {
            @Override
            public boolean apply(Class<?> input) {
                return Asset.class.isAssignableFrom(input) && input != Asset.class;
            }
        })) {
            subtypes.put((Class<? extends Asset>) parentType, type);
            Collections.sort(subtypes.get((Class<? extends Asset>) parentType), new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    return o1.getSimpleName().compareTo(o2.getSimpleName());
                }
            });
        }
    }

    private void removeSubtypesFor(Class<? extends Asset> type) {
        for (Class<?> parentType : ReflectionUtils.getAllSuperTypes(type, new Predicate<Class<?>>() {
            @Override
            public boolean apply(Class<?> input) {
                return Asset.class.isAssignableFrom(input) && input != Asset.class;
            }
        })) {
            subtypes.remove(parentType, type);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> Optional<ModuleAssetDataProducer<U>> getModuleProducerFor(Class<T> type) {
        return Optional.fromNullable((ModuleAssetDataProducer<U>) moduleProducers.get(type));
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public ModuleEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(ModuleEnvironment environment) {
        Preconditions.checkNotNull(environment);
        this.environment = environment;

        removeExtensionTypes();
        removeExtensionProducers();
        removeExtensionFormats();
        removeExtensionSupplementalFormats();
        removeExtensionDeltaFormats();

        scanForExtensionAssetTypes();
        scanForExtensionProducers();
        scanForExtensionFormats();
        scanForExtensionSupplementalFormats();
        scanForExtensionDeltaFormats();

        updateEnvironment();
    }

    public void reloadChanged() {
        for (Map.Entry<Class<? extends Asset>, ModuleAssetDataProducer<?>> entry : moduleProducers.entrySet()) {
            reloadChanged(entry.getKey(), entry.getValue());
        }
    }

    private <T extends AssetData> void reloadChanged(Class<? extends Asset> type, ModuleAssetDataProducer<T> producer) {
        Set<ResourceUrn> changedUrns = producer.checkForChanges();
        if (!changedUrns.isEmpty()) {
            AssetType<?, T> assetType = getAssetType(type);
            for (ResourceUrn changedUrn : changedUrns) {
                if (assetType.isLoaded(changedUrn)) {
                    try {
                        logger.info("Reloading changed asset '{}'", changedUrn);
                        Optional<T> assetData = producer.getAssetData(changedUrn);
                        if (assetData.isPresent()) {
                            assetType.loadAsset(changedUrn, assetData.get());
                        }
                    } catch (IOException e) {
                        logger.error("Failed to reload asset '{}'", changedUrn, e);
                    }
                }
            }
        }
    }

    private void updateEnvironment() {
        for (ModuleAssetDataProducer<?> producer : moduleProducers.values()) {
            producer.setEnvironment(environment);
        }
        for (AssetType<?, ?> assetType : assetTypes.values()) {
            assetType.refresh();
        }
    }

    private void removeExtensionTypes() {
        for (AssetType<?, ?> type : extensionAssetTypes) {
            removeCoreAssetType(type.getAssetClass());
        }
        extensionAssetTypes.clear();
    }

    @SuppressWarnings("unchecked")
    private void removeExtensionProducers() {
        for (Map.Entry<AssetDataProducer<?>, AssetType<?, ?>> entry : extensionProducers.entries()) {
            entry.getValue().removeProducer((AssetDataProducer) entry.getKey());
        }
        extensionProducers.clear();
    }

    @SuppressWarnings("unchecked")
    private void removeExtensionFormats() {
        for (Map.Entry<AssetFileFormat<?>, AssetType<?, ?>> entry : extensionFormats.entries()) {
            ModuleAssetDataProducer<?> moduleProducer = moduleProducers.get(entry.getValue().getAssetClass());
            if (moduleProducer != null) {
                moduleProducer.removeAssetFormat((AssetFileFormat) entry.getKey());
            }
        }
        extensionFormats.clear();
    }

    @SuppressWarnings("unchecked")
    private void removeExtensionDeltaFormats() {
        for (Map.Entry<AssetAlterationFileFormat<?>, AssetType<?, ?>> entry : extensionSupplementalFormats.entries()) {
            ModuleAssetDataProducer<?> moduleProducer = moduleProducers.get(entry.getValue().getAssetClass());
            if (moduleProducer != null) {
                moduleProducer.removeSupplementFormat((AssetAlterationFileFormat) entry.getKey());
            }
        }
        extensionSupplementalFormats.clear();
    }

    @SuppressWarnings("unchecked")
    private void removeExtensionSupplementalFormats() {
        for (Map.Entry<AssetAlterationFileFormat<?>, AssetType<?, ?>> entry : extensionDeltaFormats.entries()) {
            ModuleAssetDataProducer<?> moduleProducer = moduleProducers.get(entry.getValue().getAssetClass());
            if (moduleProducer != null) {
                moduleProducer.removeDeltaFormat((AssetAlterationFileFormat) entry.getKey());
            }
        }
        extensionDeltaFormats.clear();
    }

    @SuppressWarnings("unchecked")
    private void scanForExtensionAssetTypes() {
        for (AssetFactory factory : findAndCreateClasses(AssetFactory.class, RegisterAssetType.class)) {
            Optional<Type> assetClassType = GenericsUtil.getTypeParameterBindingForInheritedClass(factory.getClass(), AssetFactory.class, 0);
            if (!assetClassType.isPresent()) {
                logger.error("Could not register AssetType with factory '{}' - asset type must be bound in inheritance tree", factory.getClass());
                continue;
            }
            Class<? extends Asset> assetClass = (Class<? extends Asset>) GenericsUtil.getClassOfType(assetClassType.get());
            RegisterAssetType registrationInfo = factory.getClass().getAnnotation(RegisterAssetType.class);
            AssetType<?, ?> assetType;
            if (!registrationInfo.value().isEmpty()) {
                assetType = registerCoreAssetType(assetClass, factory, registrationInfo.value());
            } else {
                assetType = registerCoreAssetType(assetClass, factory);
            }
            extensionAssetTypes.add(assetType);
        }
    }

    @SuppressWarnings("unchecked")
    private void scanForExtensionProducers() {
        for (AssetDataProducer producer : findAndCreateClasses(AssetDataProducer.class, RegisterAssetDataProducer.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(producer.getClass(), AssetDataProducer.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register AssetProducer '{}' - asset data type must be bound in inheritance tree", producer.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            List<AssetType<?, ?>> validAssetTypes = Lists.newArrayList(Collections2.filter(assetTypes.values(), new Predicate<AssetType<?, ?>>() {
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
                    if (!extensionAssetTypes.contains(assetType)) {
                        extensionProducers.put(producer, assetType);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void scanForExtensionFormats() {
        for (AssetFileFormat format : findAndCreateClasses(AssetFileFormat.class, RegisterAssetFileFormat.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(format.getClass(), AssetFileFormat.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register AssetFormat '{}' - asset data type must be bound in inheritance tree", format.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            List<AssetType<?, ?>> validAssetTypes = Lists.newArrayList(Collections2.filter(assetTypes.values(), new Predicate<AssetType<?, ?>>() {
                @Override
                public boolean apply(AssetType<?, ?> input) {
                    return input.getAssetDataClass().equals(assetDataClass);
                }
            }));
            if (validAssetTypes.isEmpty()) {
                logger.error("No asset type available for asset format {}", format.getClass());
            } else {
                for (AssetType assetType : validAssetTypes) {
                    ModuleAssetDataProducer moduleProducer = moduleProducers.get(assetType.getAssetClass());
                    if (moduleProducer != null) {
                        moduleProducer.addAssetFormat(format);
                        if (!extensionAssetTypes.contains(assetType)) {
                            extensionFormats.put(format, assetType);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void scanForExtensionSupplementalFormats() {
        for (AssetAlterationFileFormat format : findAndCreateClasses(AssetAlterationFileFormat.class, RegisterAssetSupplementalFileFormat.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(format.getClass(), AssetAlterationFileFormat.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register Asset Supplemental Format '{}' - asset data type must be bound in inheritance tree", format.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            List<AssetType<?, ?>> validAssetTypes = Lists.newArrayList(Collections2.filter(assetTypes.values(), new Predicate<AssetType<?, ?>>() {
                @Override
                public boolean apply(AssetType<?, ?> input) {
                    return input.getAssetDataClass().equals(assetDataClass);
                }
            }));
            if (validAssetTypes.isEmpty()) {
                logger.error("No asset type available for asset supplemental format {}", format.getClass());
            } else {
                for (AssetType assetType : validAssetTypes) {
                    ModuleAssetDataProducer moduleProducer = moduleProducers.get(assetType.getAssetClass());
                    if (moduleProducer != null) {
                        moduleProducer.addSupplementFormat(format);
                        if (!extensionAssetTypes.contains(assetType)) {
                            extensionSupplementalFormats.put(format, assetType);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void scanForExtensionDeltaFormats() {
        for (AssetAlterationFileFormat format : findAndCreateClasses(AssetAlterationFileFormat.class, RegisterAssetDeltaFileFormat.class)) {
            Optional<Type> assetDataType = GenericsUtil.getTypeParameterBindingForInheritedClass(format.getClass(), AssetAlterationFileFormat.class, 0);
            if (!assetDataType.isPresent()) {
                logger.error("Could not register Asset Delta Format '{}' - asset data type must be bound in inheritance tree", format.getClass());
                continue;
            }
            final Class<? extends AssetData> assetDataClass = (Class<? extends AssetData>) GenericsUtil.getClassOfType(assetDataType.get());
            List<AssetType<?, ?>> validAssetTypes = Lists.newArrayList(Collections2.filter(assetTypes.values(), new Predicate<AssetType<?, ?>>() {
                @Override
                public boolean apply(AssetType<?, ?> input) {
                    return input.getAssetDataClass().equals(assetDataClass);
                }
            }));
            if (validAssetTypes.isEmpty()) {
                logger.error("No asset type available for asset delta format {}", format.getClass());
            } else {
                for (AssetType assetType : validAssetTypes) {
                    ModuleAssetDataProducer moduleProducer = moduleProducers.get(assetType.getAssetClass());
                    if (moduleProducer != null) {
                        moduleProducer.addDeltaFormat(format);
                        if (!extensionAssetTypes.contains(assetType)) {
                            extensionDeltaFormats.put(format, assetType);
                        }
                    }
                }
            }
        }
    }

    private <T> List<T> findAndCreateClasses(final Class<T> baseType, Class<? extends Annotation> annotation) {
        List<T> result = Lists.newArrayList();
        for (Class<?> discoveredType : environment.getTypesAnnotatedWith(annotation, new Predicate<Class<?>>() {
            @Override
            public boolean apply(Class<?> input) {
                return baseType.isAssignableFrom(input) && !Modifier.isAbstract(input.getModifiers());
            }
        })) {
            T instance = null;
            try {
                Constructor<?> assetManagerConstructor = discoveredType.getConstructor(AssetManager.class);
                instance = baseType.cast(assetManagerConstructor.newInstance(assetManager));
            } catch (NoSuchMethodException e) {
                logger.debug("No asset manager constructor for {}, falling back on default constructor", discoveredType);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                logger.error("Failed to instantiate class: {}", discoveredType, e);
            }

            if (instance == null) {
                try {
                    discoveredType.getConstructor();
                } catch (NoSuchMethodException e) {
                    logger.error("Type '" + discoveredType + "' missing usable constructor");
                    continue;
                }
                try {
                    instance = baseType.cast(discoveredType.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("Failed to instantiate class: {}", discoveredType, e);
                }
            }
            if (instance != null) {
                result.add(instance);
            }
        }
        return result;
    }

}
