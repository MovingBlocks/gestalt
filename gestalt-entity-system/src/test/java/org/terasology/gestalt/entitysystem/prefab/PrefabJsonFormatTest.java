// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.entitysystem.prefab;

import modules.test.components.Reference;
import modules.test.components.Sample;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.format.producer.AssetFileDataProducer;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.gestalt.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.gestalt.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.gestalt.assets.module.ModuleDependencyResolutionStrategy;
import org.terasology.gestalt.assets.module.ModuleEnvironmentDependencyProvider;
import org.terasology.gestalt.entitysystem.component.management.ComponentManager;
import org.terasology.gestalt.entitysystem.component.management.ComponentTypeIndex;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.module.ModuleFactory;
import org.terasology.gestalt.module.sandbox.PermitAllPermissionProviderFactory;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class PrefabJsonFormatTest {

    private static final ResourceUrn SINGLE_URN = new ResourceUrn("test:single");
    private static final ResourceUrn ABRIDGED_SINGLE_URN = new ResourceUrn("test:abridged-single");
    private static final ResourceUrn MULTI_URN = new ResourceUrn("test:multi");
    private static final ResourceUrn MULTI_ROOT_LAST_URN = new ResourceUrn("test:multi-root-last");
    private static final ResourceUrn LENIENT_URN = new ResourceUrn("test:lenient-single");
    private static final ResourceUrn MULTI_EXPLICT_ROOT_URN = new ResourceUrn("test:multi-explicit-root");
    private static final ResourceUrn EXTERNAL_COMPOSITION_URN = new ResourceUrn("test:external-composition");
    private static final ResourceUrn INHERITANCE_URN = new ResourceUrn("test:inheritance");
    private static final ResourceUrn REFERENCE_LIST_URN = new ResourceUrn("test:reference-list");

    private static final String ROOT_FRAGMENT = "root";
    private static final String SECOND_FRAGMENT = "second";
    private static final String TEST_NAME = "Test Name";
    private static final String TEST_NAME_2 = "Second Entity";


    private ModuleEnvironment moduleEnvironment;

    private ComponentManager componentManager;

    private ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManagerImpl();
    private AssetManager assetManager = new AssetManager(assetTypeManager);

    public PrefabJsonFormatTest() throws Exception {
        ModuleFactory factory = new ModuleFactory();
        Module module = factory.createPackageModule("modules.test");
        ModuleEnvironment moduleEnvironment = new ModuleEnvironment(Collections.singletonList(module), new PermitAllPermissionProviderFactory());

        componentManager = new ComponentManager();
        AssetType<Prefab, PrefabData> prefabAssetType = assetTypeManager.createAssetType(Prefab.class, Prefab::new, "prefabs");
        AssetFileDataProducer<PrefabData> prefabDataProducer = assetTypeManager.getAssetFileDataProducer(prefabAssetType);
        ComponentTypeIndex componentTypeIndex = new ComponentTypeIndex(moduleEnvironment, new ModuleDependencyResolutionStrategy(new ModuleEnvironmentDependencyProvider(moduleEnvironment)));
        prefabDataProducer.addAssetFormat(new PrefabJsonFormat.Builder(componentTypeIndex, componentManager, assetManager).create());
        assetTypeManager.switchEnvironment(moduleEnvironment);
    }

    @Test
    public void loadSimplePrefab() {
        Optional<Prefab> result = assetManager.getAsset(SINGLE_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();
        ResourceUrn rootUrn = new ResourceUrn(SINGLE_URN, ROOT_FRAGMENT);
        assertEquals(rootUrn, prefab.getRootEntityUrn());
        assertEquals(1, prefab.getEntityRecipes().size());
        assertTrue(prefab.getEntityRecipes().containsKey(rootUrn));
        assertEquals(1, prefab.getEntityRecipes().get(rootUrn).getComponents().size());
        assertTrue(prefab.getEntityRecipes().get(rootUrn).getComponent(Sample.class).isPresent());
        assertEquals(TEST_NAME, prefab.getEntityRecipes().get(rootUrn).getComponent(Sample.class).get().getName());
    }

    @Test
    public void loadAbridgedSingle() {
        Optional<Prefab> result = assetManager.getAsset(ABRIDGED_SINGLE_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();
        ResourceUrn rootUrn = new ResourceUrn(ABRIDGED_SINGLE_URN, ROOT_FRAGMENT);
        assertEquals(rootUrn, prefab.getRootEntityUrn());
        assertEquals(1, prefab.getEntityRecipes().size());
        assertTrue(prefab.getEntityRecipes().containsKey(rootUrn));
        assertEquals(1, prefab.getEntityRecipes().get(rootUrn).getComponents().size());
        assertTrue(prefab.getEntityRecipes().get(rootUrn).getComponent(Sample.class).isPresent());
        assertEquals(TEST_NAME, prefab.getEntityRecipes().get(rootUrn).getComponent(Sample.class).get().getName());
    }

    @Test
    public void loadMultiEntityPrefab() {
        Optional<Prefab> result = assetManager.getAsset(MULTI_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();

        ResourceUrn rootUrn = new ResourceUrn(MULTI_URN, ROOT_FRAGMENT);
        ResourceUrn secondUrn = new ResourceUrn(MULTI_URN, SECOND_FRAGMENT);
        assertEquals(rootUrn, prefab.getRootEntityUrn());
        assertEquals(2, prefab.getEntityRecipes().size());
        assertTrue(prefab.getEntityRecipes().containsKey(rootUrn));
        assertTrue(prefab.getEntityRecipes().containsKey(secondUrn));

        EntityRecipe secondEntity = prefab.getEntityRecipes().get(secondUrn);
        assertEquals(1, secondEntity.getComponents().size());
        assertTrue(secondEntity.getComponent(Sample.class).isPresent());
        assertEquals(TEST_NAME_2, secondEntity.getComponent(Sample.class).get().getName());

        EntityRecipe rootEntity = prefab.getEntityRecipes().get(rootUrn);
        assertEquals(2, rootEntity.getComponents().size());
        assertTrue(rootEntity.getComponent(Sample.class).isPresent());
        assertEquals(TEST_NAME, rootEntity.getComponent(Sample.class).get().getName());
        assertTrue(rootEntity.getComponent(Reference.class).isPresent());
        assertEquals(secondEntity.getReference(), rootEntity.getComponent(Reference.class).get().getReference());
    }

    @Test
    public void loadMultiEntityWithRootLast() {
        Optional<Prefab> result = assetManager.getAsset(MULTI_ROOT_LAST_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();

        ResourceUrn rootUrn = new ResourceUrn(MULTI_ROOT_LAST_URN, ROOT_FRAGMENT);

        assertEquals(rootUrn, prefab.getRootEntityUrn());
    }

    @Test
    public void loadPrefabIsLenient() {
        Optional<Prefab> result = assetManager.getAsset(LENIENT_URN, Prefab.class);
        assertTrue(result.isPresent());
    }

    @Test
    public void loadMultiEntityWithExplicitRoot() {
        Optional<Prefab> result = assetManager.getAsset(MULTI_EXPLICT_ROOT_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();

        ResourceUrn rootUrn = new ResourceUrn(MULTI_EXPLICT_ROOT_URN, "first");

        assertEquals(rootUrn, prefab.getRootEntityUrn());
    }

    @Test
    public void loadEntityWithExternalReference() {
        Optional<Prefab> result = assetManager.getAsset(EXTERNAL_COMPOSITION_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();

        Reference referenceComponent = prefab.getEntityRecipes().get(prefab.getRootEntityUrn()).getComponent(Reference.class).orElseThrow(() -> new RuntimeException("Expected Reference"));
        assertTrue(referenceComponent.getReference() instanceof PrefabRef);
        assertEquals(SINGLE_URN, ((PrefabRef) referenceComponent.getReference()).getPrefab().getUrn());
    }

    @Test
    public void loadEntityWithInheritance() {
        Optional<Prefab> result = assetManager.getAsset(INHERITANCE_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();
        EntityRecipe recipe = prefab.getRootEntity();
        Sample sample = recipe.getComponent(Sample.class).orElseThrow(() -> new RuntimeException("Expected Sample"));
        assertEquals(TEST_NAME, sample.getName());
        assertEquals("New Description", sample.getDescription());
    }

    @Test
    public void loadEntityWithReferenceList() {
        Optional<Prefab> result = assetManager.getAsset(REFERENCE_LIST_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();
        EntityRecipe rootEntity = prefab.getRootEntity();
        Reference component = rootEntity.getComponent(Reference.class).orElseThrow(() -> new RuntimeException("Expected Reference"));
        assertEquals(3, component.getReferences().size());
    }


}
