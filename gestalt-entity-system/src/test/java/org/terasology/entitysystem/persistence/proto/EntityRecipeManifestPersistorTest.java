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

import com.google.common.collect.Sets;

import org.junit.Test;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.producer.AssetFileDataProducer;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.entitysystem.component.CodeGenComponentManager;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.persistence.proto.persistors.ComponentPersistor;
import org.terasology.entitysystem.persistence.proto.persistors.EntityRecipeManifestPersistor;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.entitysystem.prefab.PrefabData;
import org.terasology.entitysystem.prefab.PrefabJsonFormat;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.util.Collections;

import modules.test.SampleComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class EntityRecipeManifestPersistorTest {

    private static final ResourceUrn ENTITY_RECIPE_URN = new ResourceUrn("test", "single", "root");
    private static final ResourceUrn PREFAB_URN = ENTITY_RECIPE_URN.getRootUrn();
    private static final ResourceUrn UNRESOLVABLE_RECIPE_URN = new ResourceUrn("test", "fakey", "fake");
    private static final int ID = 1;

    private ComponentManager componentManager;
    private EntityRecipeManifestPersistor persistor;
    private ProtoPersistence context = ProtoPersistence.create();

    private ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManagerImpl();
    private AssetManager assetManager = new AssetManager(assetTypeManager);

    private EntityRecipeManifest manifest = new EntityRecipeManifest(assetManager);
    private EntityRecipe entityRecipeFromPrefab;
    private EntityRecipe entityRecipeUnresolvable;

    public EntityRecipeManifestPersistorTest() throws Exception {
        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        componentManager = new CodeGenComponentManager(typeLibrary);

        ModuleFactory factory = new ModuleFactory();
        Module module = factory.createPackageModule("modules.test");
        ModuleEnvironment moduleEnvironment = new ModuleEnvironment(Collections.singletonList(module), new PermitAllPermissionProviderFactory());

        AssetType<Prefab, PrefabData> prefabAssetType = assetTypeManager.createAssetType(Prefab.class, Prefab::new, "prefabs");
        AssetFileDataProducer<PrefabData> prefabDataProducer = assetTypeManager.getAssetFileDataProducer(prefabAssetType);
        prefabDataProducer.addAssetFormat(new PrefabJsonFormat.Builder(moduleEnvironment, componentManager, assetManager).create());
        assetTypeManager.switchEnvironment(moduleEnvironment);

        ComponentPersistor componentPersistor = new ComponentPersistor(context, new ComponentManifest(moduleEnvironment, componentManager));
        persistor = new EntityRecipeManifestPersistor(assetManager, componentPersistor);

        entityRecipeFromPrefab = assetManager.getAsset(PREFAB_URN, Prefab.class).get().getEntityRecipes().get(ENTITY_RECIPE_URN);
        entityRecipeUnresolvable = new EntityRecipe(UNRESOLVABLE_RECIPE_URN);
        SampleComponent comp = componentManager.create(SampleComponent.class);
        comp.setName("Fake");
        comp.setDescription("Fakey fake");
        entityRecipeUnresolvable.add(SampleComponent.class, comp);
    }

    @Test
    public void persistEntityRecipeWhenResolvable() {
        manifest.addEntityRecipeMetadata(new EntityRecipeMetadata(ID, ENTITY_RECIPE_URN, entityRecipeFromPrefab));
        EntityRecipeManifest finalManifest = persistor.deserialize(persistor.serialize(manifest).build());
        EntityRecipeMetadata entityRecipeMetadata = finalManifest.getEntityRecipeMetadata(ID).orElseThrow(AssertionError::new);

        assertNotNull(entityRecipeMetadata);
        assertEquals(Sets.newHashSet(entityRecipeFromPrefab.getComponents().values()), Sets.newHashSet(entityRecipeMetadata.getComponents().values()));
    }

    @Test
    public void persistEntityRecipeWhenResolvableAndChanged() {
        EntityRecipe entityRecipeFromPrefab = assetManager.getAsset(PREFAB_URN, Prefab.class).get().getEntityRecipes().get(ENTITY_RECIPE_URN);
        manifest.addEntityRecipeMetadata(new EntityRecipeMetadata(ID, ENTITY_RECIPE_URN, entityRecipeFromPrefab));
        ProtoDatastore.EntityRecipeManifestData serializedManifest = persistor.serialize(manifest).build();
        entityRecipeFromPrefab.getComponent(SampleComponent.class).get().setName("Altered Name");
        EntityRecipeManifest finalManifest = persistor.deserialize(serializedManifest);
        EntityRecipeMetadata entityRecipeMetadata = finalManifest.getEntityRecipeMetadata(ID).orElseThrow(AssertionError::new);

        assertNotNull(entityRecipeMetadata);
        assertEquals(Sets.newHashSet(entityRecipeFromPrefab.getComponents().values()), Sets.newHashSet(entityRecipeMetadata.getComponents().values()));
    }

    @Test
    public void persistEntityRecipeWhenEntityRecipeNotResolvable() {
        manifest.addEntityRecipeMetadata(new EntityRecipeMetadata(ID, UNRESOLVABLE_RECIPE_URN, entityRecipeUnresolvable));
        ProtoDatastore.EntityRecipeManifestData serializedManifest = persistor.serialize(manifest).build();
        EntityRecipeManifest finalManifest = persistor.deserialize(serializedManifest);
        EntityRecipeMetadata entityRecipeMetadata = finalManifest.getEntityRecipeMetadata(ID).orElseThrow(AssertionError::new);

        assertNotNull(entityRecipeMetadata);
        assertEquals(Sets.newHashSet(entityRecipeUnresolvable.getComponents().values()), Sets.newHashSet(entityRecipeMetadata.getComponents().values()));
    }

    @Test
    public void persistEntityRecipeWhenPrefabNotResolvable() {
        manifest.addEntityRecipeMetadata(new EntityRecipeMetadata(ID, entityRecipeUnresolvable.getIdentifier(), entityRecipeUnresolvable));
        ProtoDatastore.EntityRecipeManifestData serializedManifest = persistor.serialize(manifest).build();
        EntityRecipeManifest finalManifest = persistor.deserialize(serializedManifest);
        EntityRecipeMetadata entityRecipeMetadata = finalManifest.getEntityRecipeMetadata(ID).orElseThrow(AssertionError::new);

        assertNotNull(entityRecipeMetadata);
        assertEquals(Sets.newHashSet(entityRecipeUnresolvable.getComponents().values()), Sets.newHashSet(entityRecipeMetadata.getComponents().values()));
    }

}
