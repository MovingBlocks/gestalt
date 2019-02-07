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

package org.terasology.entitysystem.prefab;

import org.junit.Test;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.producer.AssetFileDataProducer;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import virtualModules.test.VirtualModuleEnvironmentFactory;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.stubs.ReferenceComponent;
import org.terasology.entitysystem.stubs.SampleComponent;
import org.terasology.module.ModuleEnvironment;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    private ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManager();
    private AssetManager assetManager = new AssetManager(assetTypeManager);

    public PrefabJsonFormatTest() throws Exception {
        VirtualModuleEnvironmentFactory virtualModuleEnvironmentFactory = new VirtualModuleEnvironmentFactory(getClass());
        moduleEnvironment = virtualModuleEnvironmentFactory.createEnvironment();

        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        typeLibrary.addHandler(new TypeHandler<>(EntityRef.class, ImmutableCopy.create()));
        componentManager = new CodeGenComponentManager(typeLibrary);
        AssetType<Prefab, PrefabData> prefabAssetType = assetTypeManager.createAssetType(Prefab.class, Prefab::new, "prefabs");
        AssetFileDataProducer<PrefabData> prefabDataProducer = assetTypeManager.getAssetFileDataProducer(prefabAssetType);
        prefabDataProducer.addAssetFormat(new PrefabJsonFormat.Builder(moduleEnvironment, componentManager, assetManager).create());

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
        assertTrue(prefab.getEntityRecipes().get(rootUrn).getComponent(SampleComponent.class).isPresent());
        assertEquals(TEST_NAME, prefab.getEntityRecipes().get(rootUrn).getComponent(SampleComponent.class).get().getName());
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
        assertTrue(prefab.getEntityRecipes().get(rootUrn).getComponent(SampleComponent.class).isPresent());
        assertEquals(TEST_NAME, prefab.getEntityRecipes().get(rootUrn).getComponent(SampleComponent.class).get().getName());
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
        assertTrue(secondEntity.getComponent(SampleComponent.class).isPresent());
        assertEquals(TEST_NAME_2, secondEntity.getComponent(SampleComponent.class).get().getName());

        EntityRecipe rootEntity = prefab.getEntityRecipes().get(rootUrn);
        assertEquals(2, rootEntity.getComponents().size());
        assertTrue(rootEntity.getComponent(SampleComponent.class).isPresent());
        assertEquals(TEST_NAME, rootEntity.getComponent(SampleComponent.class).get().getName());
        assertTrue(rootEntity.getComponent(ReferenceComponent.class).isPresent());
        assertEquals(secondEntity, rootEntity.getComponent(ReferenceComponent.class).get().getReference());
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

        ReferenceComponent referenceComponent = prefab.getEntityRecipes().get(prefab.getRootEntityUrn()).getComponent(ReferenceComponent.class).orElseThrow(() -> new RuntimeException("Expected ReferenceComponent"));
        assertTrue(referenceComponent.getReference() instanceof PrefabRef);
        assertEquals(SINGLE_URN, ((PrefabRef) referenceComponent.getReference()).getPrefab().getUrn());
    }

    @Test
    public void loadEntityWithInheritance() {
        Optional<Prefab> result = assetManager.getAsset(INHERITANCE_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();
        EntityRecipe recipe = prefab.getRootEntity();
        SampleComponent sample = recipe.getComponent(SampleComponent.class).orElseThrow(() -> new RuntimeException("Expected SampleComponent"));
        assertEquals(TEST_NAME, sample.getName());
        assertEquals("New Description", sample.getDescription());
    }

    @Test
    public void loadEntityWithReferenceList() {
        Optional<Prefab> result = assetManager.getAsset(REFERENCE_LIST_URN, Prefab.class);
        assertTrue(result.isPresent());
        Prefab prefab = result.get();
        EntityRecipe rootEntity = prefab.getRootEntity();
        ReferenceComponent component = rootEntity.getComponent(ReferenceComponent.class).orElseThrow(() -> new RuntimeException("Expected ReferenceComponent"));
        assertEquals(3, component.getReferences().size());
    }


}
