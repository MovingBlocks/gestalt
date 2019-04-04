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

package org.terasology.entitysystem.persistence.proto;

import org.junit.Test;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.producer.AssetFileDataProducer;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.persistence.proto.persistors.EntityPersistor;
import org.terasology.entitysystem.persistence.proto.persistors.PrefabAwareEntityPersistor;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.entitysystem.prefab.GeneratedFromEntityRecipeComponent;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.entitysystem.prefab.PrefabData;
import org.terasology.entitysystem.prefab.PrefabJsonFormat;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;

import java.util.Collections;
import java.util.Optional;

import modules.test.components.Sample;
import modules.test.components.Second;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class PrefabAwareEntityPersistorTest {

    private static final String NAME = "Test Name";
    private static final String DESCRIPTION = "Test Description";
    private static final ResourceUrn EXISTING_PREFAB_URN = new ResourceUrn("test", "single");
    private static final ResourceUrn EXISTING_ENTITY_RECIPE_URN = new ResourceUrn("test", "single", "root");
    private static final String ALTERED_NAME = "Altered Name";
    private static final ResourceUrn TEMP_ENTITY_RECIPE_URN = new ResourceUrn("test", "temp", "root");
    private static final ResourceUrn TEMP_PREFAB_URN = new ResourceUrn("test", "temp");

    private ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManagerImpl();
    private AssetManager assetManager = new AssetManager(assetTypeManager);
    private ComponentManager componentManager;
    private ProtoPersistence context = ProtoPersistence.create();
    private TransactionManager originalTransaction = new TransactionManager();
    private TransactionManager finalTransaction = new TransactionManager();

    private EntityPersistor persistor;
    private EntityManager originalEntityManager;
    private EntityManager finalEntityManager;

    public PrefabAwareEntityPersistorTest() throws Exception {
        componentManager = new ComponentManager();

        ModuleFactory factory = new ModuleFactory();
        Module module = factory.createPackageModule("modules.test");
        ModuleEnvironment moduleEnvironment = new ModuleEnvironment(Collections.singletonList(module), new PermitAllPermissionProviderFactory());

        AssetType<Prefab, PrefabData> prefabAssetType = assetTypeManager.createAssetType(Prefab.class, Prefab::new, "prefabs");
        AssetFileDataProducer<PrefabData> prefabDataProducer = assetTypeManager.getAssetFileDataProducer(prefabAssetType);
        prefabDataProducer.addAssetFormat(new PrefabJsonFormat.Builder(moduleEnvironment, componentManager, assetManager).create());
        assetTypeManager.switchEnvironment(moduleEnvironment);
        persistor = new PrefabAwareEntityPersistor(componentManager, context, new ComponentManifest(moduleEnvironment, componentManager), new EntityRecipeManifest(assetManager));

        originalEntityManager = new InMemoryEntityManager(componentManager, originalTransaction);
        finalEntityManager = new InMemoryEntityManager(componentManager, finalTransaction, 5);
    }

    private EntityRef persist(EntityRef entity) {
        originalTransaction.begin();
        finalTransaction.begin();
        EntityRef finalEntity = persistor.deserialize(persistor.serialize(entity).build(), finalEntityManager);
        originalTransaction.rollback();
        finalTransaction.commit();
        return finalEntity;
    }

    @Test
    public void persistTrivialEntity() {
        originalTransaction.begin();
        EntityRef entity = originalEntityManager.createEntity();
        Sample component = entity.addComponent(Sample.class);
        component.setName(NAME);
        component.setDescription(DESCRIPTION);
        originalTransaction.commit();

        EntityRef finalEntity = persist(entity);

        finalTransaction.begin();
        assertEquals(entity.getId(), finalEntity.getId());

        Sample comp = finalEntity.getComponent(Sample.class).orElseThrow(RuntimeException::new);
        assertEquals(NAME, comp.getName());
        assertEquals(DESCRIPTION, comp.getDescription());
    }

    @Test
    public void persistEntityFromPrefab() {
        originalTransaction.begin();
        EntityRef entity = originalEntityManager.createEntity(assetManager.getAsset(EXISTING_PREFAB_URN, Prefab.class).orElseThrow(AssertionError::new));
        originalTransaction.commit();

        EntityRef finalEntity = persist(entity);

        finalTransaction.begin();
        assertEquals(entity.getId(), finalEntity.getId());

        Sample comp = finalEntity.getComponent(Sample.class).orElseThrow(RuntimeException::new);
        assertEquals(NAME, comp.getName());
        assertEquals(DESCRIPTION, comp.getDescription());
    }

    @Test
    public void persistEntityFromChangedPrefab() {
        Prefab prefab = assetManager.getAsset(EXISTING_PREFAB_URN, Prefab.class).orElseThrow(AssertionError::new);
        originalTransaction.begin();
        EntityRef entity = originalEntityManager.createEntity(prefab);
        originalTransaction.commit();

        originalTransaction.begin();
        ProtoDatastore.EntityData data = persistor.serialize(entity).build();
        originalTransaction.rollback();

        prefab.getRootEntity().getComponent(Sample.class).orElseThrow(AssertionError::new).setName(ALTERED_NAME);

        finalTransaction.begin();
        EntityRef finalEntity = persistor.deserialize(data, finalEntityManager);
        assertEquals(entity.getId(), finalEntity.getId());

        Sample comp = finalEntity.getComponent(Sample.class).orElseThrow(RuntimeException::new);
        assertEquals(ALTERED_NAME, comp.getName());
    }

    @Test
    public void persistEntityFromRemovedPrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe recipe = new EntityRecipe(TEMP_ENTITY_RECIPE_URN);

        Sample comp = componentManager.create(Sample.class);
        comp.setName(NAME);
        recipe.add(Sample.class, comp);

        prefabData.addEntityRecipe(recipe);
        Prefab prefab = assetManager.loadAsset(TEMP_PREFAB_URN, prefabData, Prefab.class);

        originalTransaction.begin();
        EntityRef entity = originalEntityManager.createEntity(prefab);
        originalTransaction.commit();
        originalTransaction.begin();
        ProtoDatastore.EntityData data = persistor.serialize(entity).build();
        originalTransaction.rollback();

        prefab.dispose();

        finalTransaction.begin();
        EntityRef finalEntity = persistor.deserialize(data, finalEntityManager);
        assertEquals(entity.getId(), finalEntity.getId());

        Sample finalComp = finalEntity.getComponent(Sample.class).orElseThrow(AssertionError::new);
        assertEquals(comp, finalComp);
    }

    @Test
    public void persistRemovedComponents() {
        originalTransaction.begin();
        EntityRef entity = originalEntityManager.createEntity(assetManager.getAsset(EXISTING_PREFAB_URN, Prefab.class).orElseThrow(AssertionError::new));
        entity.addComponent(Second.class);
        entity.removeComponent(Sample.class);
        originalTransaction.commit();

        EntityRef finalEntity = persist(entity);

        finalTransaction.begin();
        assertFalse(finalEntity.getComponent(Sample.class).isPresent());
        assertTrue(finalEntity.getComponent(Second.class).isPresent());
    }

    // persist restores / drops CreatedFromPrefabComponent as needed
    @Test
    public void persistProducesPrefabCreatedComponentIfPrefabPresent() {
        originalTransaction.begin();
        EntityRef entity = originalEntityManager.createEntity(assetManager.getAsset(EXISTING_PREFAB_URN, Prefab.class).orElseThrow(AssertionError::new));
        originalTransaction.commit();

        EntityRef finalEntity = persist(entity);

        finalTransaction.begin();
        Optional<GeneratedFromEntityRecipeComponent> component = finalEntity.getComponent(GeneratedFromEntityRecipeComponent.class);
        assertTrue(component.isPresent());
        assertEquals(EXISTING_ENTITY_RECIPE_URN, component.get().getEntityRecipe());
    }

    // persist from missing prefab

}
