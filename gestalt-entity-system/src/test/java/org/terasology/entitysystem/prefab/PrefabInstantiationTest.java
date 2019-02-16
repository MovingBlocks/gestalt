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
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.management.MapAssetTypeManager;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.core.NullEntityRef;
import org.terasology.entitysystem.core.ProxyEntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import modules.test.ReferenceComponent;
import modules.test.SampleComponent;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.io.IOException;

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

    private TransactionManager transactionManager = new TransactionManager();
    private EntityManager entityManager;
    private ComponentManager componentManager;

    private MapAssetTypeManager assetTypeManager = new MapAssetTypeManager();
    private AssetManager assetManager = new AssetManager(assetTypeManager);
    private AssetType<Prefab, PrefabData> prefabAssetType = assetTypeManager.createAssetType(Prefab.class, Prefab::new);
    private Prefab singlePrefab;
    private Prefab multiPrefab;
    private Prefab compositePrefab;

    public PrefabInstantiationTest() {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        typeLibrary.addHandler(new TypeHandler<>(EntityRef.class, ImmutableCopy.create()));
        componentManager = new CodeGenComponentManager(typeLibrary);
        entityManager = new InMemoryEntityManager(componentManager, transactionManager);

        createSinglePrefab();
        createMultiPrefab();
        createCompositePrefab();
    }

    private void createMultiPrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe secondEntityRecipe = new EntityRecipe(SECOND_ENTITY_URN);
        SampleComponent sampleComponent = componentManager.create(SampleComponent.class);
        sampleComponent.setName(TEST_NAME);
        secondEntityRecipe.add(SampleComponent.class, sampleComponent);
        prefabData.addEntityRecipe(secondEntityRecipe);
        EntityRecipe rootEntityRecipe = new EntityRecipe(MULTI_PREFAB_ROOT_ENTITY_URN);
        ReferenceComponent referenceComponent = componentManager.create(ReferenceComponent.class);
        referenceComponent.setReference(secondEntityRecipe);
        rootEntityRecipe.add(ReferenceComponent.class, referenceComponent);
        prefabData.addEntityRecipe(rootEntityRecipe);
        prefabData.setRootEntityId(MULTI_PREFAB_ROOT_ENTITY_URN);

        multiPrefab = assetManager.loadAsset(MULTI_PREFAB_URN, prefabData, Prefab.class);
    }

    private void createCompositePrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe rootEntityRecipe = new EntityRecipe(COMPOSITE_PREFAB_ROOT_ENTITY_URN);
        ReferenceComponent referenceComponent = componentManager.create(ReferenceComponent.class);
        referenceComponent.setReference(new PrefabRef(singlePrefab));
        rootEntityRecipe.add(ReferenceComponent.class, referenceComponent);
        prefabData.addEntityRecipe(rootEntityRecipe);
        prefabData.setRootEntityId(COMPOSITE_PREFAB_ROOT_ENTITY_URN);

        compositePrefab = assetManager.loadAsset(COMPOSITE_PREFAB_URN, prefabData, Prefab.class);
    }

    private void createSinglePrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe entityRecipe = new EntityRecipe(SINGLE_PREFAB_ROOT_ENTITY_URN);
        SampleComponent sampleComponent = componentManager.create(SampleComponent.class);
        sampleComponent.setName(TEST_NAME);
        entityRecipe.add(SampleComponent.class, sampleComponent);
        prefabData.addEntityRecipe(entityRecipe);
        prefabData.setRootEntityId(SINGLE_PREFAB_ROOT_ENTITY_URN);

        singlePrefab = assetManager.loadAsset(SINGLE_PREFAB_URN, prefabData, Prefab.class);
    }

    @org.junit.Before
    public void setup() {
        transactionManager.begin();
    }

    @org.junit.After
    public void teardown() throws IOException {
        while (transactionManager.isActive()) {
            transactionManager.rollback();
        }
    }

    @Test
    public void simplePrefab() {
        EntityRef entity = entityManager.createEntity(singlePrefab);
        assertTrue(entity.getComponent(SampleComponent.class).isPresent());
        assertEquals(TEST_NAME, entity.getComponent(SampleComponent.class).get().getName());
    }

    @Test
    public void prefabWithMultipleEntities() {
        EntityRef entity = entityManager.createEntity(multiPrefab);
        assertTrue(entity.getComponent(ReferenceComponent.class).isPresent());
        EntityRef secondEntity = entity.getComponent(ReferenceComponent.class).get().getReference();
        assertFalse(secondEntity instanceof EntityRecipe);
        assertTrue(secondEntity.getComponent(SampleComponent.class).isPresent());
        assertEquals(TEST_NAME, secondEntity.getComponent(SampleComponent.class).get().getName());
    }

    @Test
    public void prefabReferencingNonExistentSubEntityPrefabInstantiatesToNullEntityRef() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe entityRecipe = new EntityRecipe(SINGLE_PREFAB_ROOT_ENTITY_URN);
        EntityRecipe secondEntityRecipe = new EntityRecipe(SECOND_ENTITY_URN);
        ReferenceComponent referenceComponent = componentManager.create(ReferenceComponent.class);
        referenceComponent.setReference(secondEntityRecipe);
        entityRecipe.add(ReferenceComponent.class, referenceComponent);
        prefabData.addEntityRecipe(entityRecipe);
        prefabData.setRootEntityId(SINGLE_PREFAB_ROOT_ENTITY_URN);

        singlePrefab = assetManager.loadAsset(SINGLE_PREFAB_URN, prefabData, Prefab.class);

        EntityRef entity = entityManager.createEntity(singlePrefab);
        ReferenceComponent component = entity.getComponent(ReferenceComponent.class).get();
        assertEquals(NullEntityRef.get(), component.getReference());
    }

    @Test
    public void prefabReferencingAnotherPrefabInstantiatesBoth() {
        EntityRef entityRef = entityManager.createEntity(compositePrefab);
        EntityRef otherEntity = entityRef.getComponent(ReferenceComponent.class).orElseThrow(() -> new RuntimeException("No reference component")).getReference();
        assertEquals(ProxyEntityRef.class, otherEntity.getClass());
    }

    @Test
    public void entitiesCreatedByPrefabHaveEntityRecipeUrnTracked() {
        EntityRef entityRef = entityManager.createEntity(singlePrefab);
        GeneratedFromEntityRecipeComponent comp = entityRef.getComponent(GeneratedFromEntityRecipeComponent.class).orElseThrow(AssertionError::new);
        assertEquals(comp.getEntityRecipe(), singlePrefab.getRootEntityUrn());
    }


}
