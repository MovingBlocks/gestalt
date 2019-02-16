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
import org.terasology.assets.management.MapAssetTypeManager;
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
 * Implementation of ModuleAwareAssetTypeManager.
 *
 * @author Immortius
 */
public class ModuleAwareAssetTypeManagerImpl implements ModuleAwareAssetTypeManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleAwareAssetTypeManagerImpl.class);

    private final MapAssetTypeManager assetTypeManager = new MapAssetTypeManager();
    private final AssetManager assetManager = new AssetManager(this);
    private final ModuleEnvironmentDependencyProvider dependencyProvider = new ModuleEnvironmentDependencyProvider();
    private final ClassFactory classFactory;
    private final ModuleAssetScanner assetScanner = new ModuleAssetScanner();

    private final Map<AssetType<?, ?>, AssetTypeInfo> assetTypeInfo = Maps.newHashMap();

    public ModuleAwareAssetTypeManagerImpl() {
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

    @Override
    public void clearAvailableAssetCache() {
        assetScanner.clearCache();
    }

    /**
     * @param classFactory The factory to use to instantiate classes for automatic registration.
     */
    public ModuleAwareAssetTypeManagerImpl(ClassFactory classFactory) {
        this.classFactory = classFactory;
    }

    @Override
    public synchronized void close() {
        unloadEnvironment();
        assetTypeManager.clear();
        assetTypeInfo.clear();
        assetScanner.clearCache();
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


    @Override
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, String... subfolderNames) {
        return this.createAssetType(type, factory, Arrays.asList(subfolderNames));
    }

    @Override
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> createAssetType(Class<T> type, AssetFactory<T, U> factory, Collection<String> subfolderNames) {
        AssetType<T, U> assetType = new AssetType<>(type, factory);
        addAssetType(assetType, subfolderNames);
        return assetType;
    }

    @Override
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, String... subfolderNames) {
        return this.addAssetType(assetType, Arrays.asList(subfolderNames));
    }

    @Override
    public synchronized <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, Collection<String> subfolderNames) {
        return addAssetType(assetType, false, subfolderNames);
    }

    private <T extends Asset<U>, U extends AssetData> AssetType<T, U> addAssetType(AssetType<T, U> assetType, boolean extension, Collection<String> subfolderNames) {
        assetTypeManager.addAssetType(assetType);
        assetType.setResolutionStrategy(new ModuleDependencyResolutionStrategy(dependencyProvider));
        AssetTypeInfo info = new AssetTypeInfo(assetType, extension);
        AssetFileDataProducer<U> producer = new AssetFileDataProducer<>(dependencyProvider, subfolderNames);
        info.setFileProducer(producer);
        assetType.addProducer(producer);
        assetTypeInfo.put(assetType, info);
        return assetType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset<U>, U extends AssetData> AssetFileDataProducer<U> getAssetFileDataProducer(AssetType<T, U> assetType) {
        Preconditions.checkArgument(assetTypeInfo.containsKey(assetType));
        return (AssetFileDataProducer<U>) assetTypeInfo.get(assetType).getFileProducer();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T extends Asset<U>, U extends AssetData> void removeAssetType(Class<T> type) {
        AssetType<?, ?> assetType = assetTypeManager.removeAssetType(type);
        assetTypeInfo.remove(assetType);
        assetType.close();
    }

    @Override
    public AssetManager getAssetManager() {
        return assetManager;
    }

    @Override
    public synchronized void switchEnvironment(ModuleEnvironment newEnvironment) {
        Preconditions.checkNotNull(newEnvironment);
        unloadEnvironment();

        // Add extensions
        addExtensionAssetTypes(newEnvironment);
        addExtensionFormats(newEnvironment);
        addExtensionProducers(newEnvironment);

        registerAssetFiles(newEnvironment);
    }

    @Override
    public synchronized void unloadEnvironment() {
        if (dependencyProvider.getModuleEnvironment() != null) {
            removeExtensionAssetTypes();
            removeExtensionFormats();
            removeExtensionProducers();

            dependencyProvider.setModuleEnvironment(null);
            clearAssetFiles();
        }
    }

    @Override
    public void reloadAssets() {
        for (AssetType<?, ?> assetType : assetTypeManager.getAssetTypes()) {
            assetType.getLoadedAssetUrns().forEach(assetType::reload);
        }
    }

    private void registerAssetFiles(ModuleEnvironment newEnvironment) {
        dependencyProvider.setModuleEnvironment(newEnvironment);

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

    @SuppressWarnings("unchecked")
    private void addExtensionAssetTypes(ModuleEnvironment environment) {
        for (Class<?> type : environment.getTypesAnnotatedWith(RegisterAssetType.class, Asset.class::isAssignableFrom)) {
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
                    createExtensionAssetType(assetClass, factory.get(), Arrays.asList(registrationInfo.folderName()));
                } else {
                    logger.error("Asset Type already registered for type '{}' - discarding additional registration", assetClass);
                }
            }
        }
    }

    private <T extends Asset<U>, U extends AssetData> void createExtensionAssetType(Class<T> type, AssetFactory<T, U> factory, Collection<String> subfolderNames) {
        AssetType<T, U> assetType = new AssetType<>(type, factory);
        addAssetType(assetType, true, subfolderNames);
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
        private AssetType<?,?> assetType;
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
