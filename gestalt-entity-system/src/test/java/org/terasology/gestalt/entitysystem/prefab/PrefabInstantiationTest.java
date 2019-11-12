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

package org.terasology.gestalt.entitysystem.prefab;

import org.junit.Test;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.gestalt.assets.management.MapAssetTypeManager;
import org.terasology.gestalt.entitysystem.component.management.ComponentManager;
import org.terasology.gestalt.entitysystem.component.store.ArrayComponentStore;
import org.terasology.gestalt.entitysystem.component.store.ComponentStore;
import org.terasology.gestalt.entitysystem.entity.EntityManager;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.entity.NullEntityRef;
import org.terasology.gestalt.entitysystem.entity.manager.CoreEntityManager;

import modules.test.components.Reference;
import modules.test.components.Sample;
import modules.test.components.Second;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class PrefabInstantiationTest {

    public static final String TEST_NAME = "Fred";
    public static final String TEST_NAME_2 = "Jill";

    public static final ResourceUrn SINGLE_PREFAB_URN = new ResourceUrn("test", "prefab");
    public static final ResourceUrn SINGLE_PREFAB_ROOT_ENTITY_URN = new ResourceUrn(SINGLE_PREFAB_URN, "root");
    public static final ResourceUrn MULTI_PREFAB_URN = new ResourceUrn("test", "multi");
    public static final ResourceUrn MULTI_PREFAB_ROOT_ENTITY_URN = new ResourceUrn(MULTI_PREFAB_URN, "root");
    public static final ResourceUrn COMPOSITE_PREFAB_URN = new ResourceUrn("test", "composite");
    public static final ResourceUrn COMPOSITE_PREFAB_ROOT_ENTITY_URN = new ResourceUrn(COMPOSITE_PREFAB_URN, "root");
    public static final ResourceUrn SECOND_ENTITY_URN = new ResourceUrn(MULTI_PREFAB_URN, "second");

    private EntityManager entityManager;
    private ComponentManager componentManager;

    private MapAssetTypeManager assetTypeManager = new MapAssetTypeManager();
    private AssetManager assetManager = new AssetManager(assetTypeManager);
    private AssetType<Prefab, PrefabData> prefabAssetType = assetTypeManager.createAssetType(Prefab.class, Prefab::new);
    private Prefab singlePrefab;
    private Prefab multiPrefab;
    private Prefab compositePrefab;

    public PrefabInstantiationTest() {
        componentManager = new ComponentManager();
        ComponentStore<Sample> sampleStore = new ArrayComponentStore<>(componentManager.getType(Sample.class));
        ComponentStore<Reference> referenceStore = new ArrayComponentStore<>(componentManager.getType(Reference.class));
        ComponentStore<Second> secondStore = new ArrayComponentStore<>(componentManager.getType(Second.class));
        ComponentStore<GeneratedFromRecipeComponent> generatedStore = new ArrayComponentStore<>(componentManager.getType(GeneratedFromRecipeComponent.class));
        entityManager = new CoreEntityManager(sampleStore, referenceStore, secondStore, generatedStore);

        createSinglePrefab();
        createMultiPrefab();
        createCompositePrefab();
    }

    private void createMultiPrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe secondEntityRecipe = new EntityRecipe(SECOND_ENTITY_URN);
        Sample sampleComponent = componentManager.create(Sample.class);
        sampleComponent.setName(TEST_NAME);
        secondEntityRecipe.add(sampleComponent);
        prefabData.addEntityRecipe(secondEntityRecipe);
        EntityRecipe rootEntityRecipe = new EntityRecipe(MULTI_PREFAB_ROOT_ENTITY_URN);
        Reference referenceComponent = componentManager.create(Reference.class);
        referenceComponent.setReference(secondEntityRecipe.getReference());
        rootEntityRecipe.add(referenceComponent);
        prefabData.addEntityRecipe(rootEntityRecipe);
        prefabData.setRootEntityId(MULTI_PREFAB_ROOT_ENTITY_URN);

        multiPrefab = assetManager.loadAsset(MULTI_PREFAB_URN, prefabData, Prefab.class);
    }

    private void createCompositePrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe rootEntityRecipe = new EntityRecipe(COMPOSITE_PREFAB_ROOT_ENTITY_URN);
        Reference referenceComponent = componentManager.create(Reference.class);
        referenceComponent.setReference(new PrefabRef(singlePrefab));
        rootEntityRecipe.add(referenceComponent);
        prefabData.addEntityRecipe(rootEntityRecipe);
        prefabData.setRootEntityId(COMPOSITE_PREFAB_ROOT_ENTITY_URN);

        compositePrefab = assetManager.loadAsset(COMPOSITE_PREFAB_URN, prefabData, Prefab.class);
    }

    private void createSinglePrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe entityRecipe = new EntityRecipe(SINGLE_PREFAB_ROOT_ENTITY_URN);
        Sample sampleComponent = componentManager.create(Sample.class);
        sampleComponent.setName(TEST_NAME);
        entityRecipe.add(sampleComponent);
        prefabData.addEntityRecipe(entityRecipe);
        prefabData.setRootEntityId(SINGLE_PREFAB_ROOT_ENTITY_URN);

        singlePrefab = assetManager.loadAsset(SINGLE_PREFAB_URN, prefabData, Prefab.class);
    }

    @Test
    public void simplePrefab() {
        EntityRef entity = entityManager.createEntity(singlePrefab);
        assertTrue(entity.hasComponent(Sample.class));
        assertEquals(TEST_NAME, entity.getComponent(Sample.class).get().getName());
    }

    @Test
    public void prefabWithMultipleEntities() {
        EntityRef entity = entityManager.createEntity(multiPrefab);
        assertTrue(entity.getComponent(Reference.class).isPresent());
        EntityRef secondEntity = entity.getComponent(Reference.class).get().getReference();
        assertFalse(secondEntity instanceof EntityRecipe);
        assertTrue(secondEntity.hasComponent(Sample.class));
        assertEquals(TEST_NAME, secondEntity.getComponent(Sample.class).get().getName());
    }

    @Test
    public void prefabReferencingNonExistentSubEntityPrefabInstantiatesToNullEntityRef() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe entityRecipe = new EntityRecipe(SINGLE_PREFAB_ROOT_ENTITY_URN);
        EntityRecipe secondEntityRecipe = new EntityRecipe(SECOND_ENTITY_URN);
        Reference referenceComponent = componentManager.create(Reference.class);
        referenceComponent.setReference(secondEntityRecipe.getReference());
        entityRecipe.add(referenceComponent);
        prefabData.addEntityRecipe(entityRecipe);
        prefabData.setRootEntityId(SINGLE_PREFAB_ROOT_ENTITY_URN);

        singlePrefab = assetManager.loadAsset(SINGLE_PREFAB_URN, prefabData, Prefab.class);

        EntityRef entity = entityManager.createEntity(singlePrefab);
        Reference component = entity.getComponent(Reference.class).get();
        assertEquals(NullEntityRef.get(), component.getReference());
    }

    @Test
    public void prefabReferencingAnotherPrefabInstantiatesBoth() {
        EntityRef entityRef = entityManager.createEntity(compositePrefab);
        EntityRef otherEntity = entityRef.getComponent(Reference.class).orElseThrow(() -> new RuntimeException("No reference component")).getReference();
        assertTrue(otherEntity.hasComponent(Sample.class));
    }

    @Test
    public void entitiesCreatedByPrefabHaveEntityRecipeUrnTracked() {
        EntityRef entityRef = entityManager.createEntity(singlePrefab);
        GeneratedFromRecipeComponent comp = entityRef.getComponent(GeneratedFromRecipeComponent.class).orElseThrow(AssertionError::new);
        assertEquals(comp.getEntityRecipe(), singlePrefab.getRootEntityUrn());
    }


}
