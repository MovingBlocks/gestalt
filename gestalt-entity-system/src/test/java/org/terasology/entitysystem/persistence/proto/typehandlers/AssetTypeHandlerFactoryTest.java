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

package org.terasology.entitysystem.persistence.proto.typehandlers;

import org.junit.Test;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.entitysystem.entity.EntityRef;
import org.terasology.entitysystem.persistence.proto.ProtoPersistence;
import org.terasology.entitysystem.prefab.EntityRecipe;
import org.terasology.entitysystem.prefab.Prefab;
import org.terasology.entitysystem.prefab.PrefabData;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.valuetype.ImmutableCopy;
import org.terasology.valuetype.TypeHandler;
import org.terasology.valuetype.TypeLibrary;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class AssetTypeHandlerFactoryTest {

    private static final ResourceUrn PREFAB_URN = new ResourceUrn("test", "test");
    private static final ResourceUrn ENTITY_URN = new ResourceUrn(PREFAB_URN, "one");

    private ModuleEnvironment moduleEnvironment;
    private ProtoPersistence context = ProtoPersistence.create();
    private ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManagerImpl();
    private AssetManager assetManager = new AssetManager(assetTypeManager);

    public AssetTypeHandlerFactoryTest() throws Exception {
        ModuleFactory factory = new ModuleFactory();
        Module module = factory.createPackageModule("modules.test");
        ModuleEnvironment moduleEnvironment = new ModuleEnvironment(Collections.singletonList(module), new PermitAllPermissionProviderFactory());

        TypeLibrary typeLibrary = new TypeLibrary();
        typeLibrary.addHandler(new TypeHandler<>(String.class, ImmutableCopy.create()));
        typeLibrary.addHandler(new TypeHandler<>(EntityRef.class, ImmutableCopy.create()));

        assetTypeManager.createAssetType(Prefab.class, Prefab::new, "prefabs");
        assetTypeManager.switchEnvironment(moduleEnvironment);

        context.addTypeHandlerFactory(new AssetTypeHandlerFactory(assetManager));
    }

    @Test
    public void serializePrefab() {
        PrefabData prefabData = new PrefabData();
        EntityRecipe entityRecipe = new EntityRecipe(ENTITY_URN);
        prefabData.addEntityRecipe(entityRecipe);
        Prefab prefab = assetManager.loadAsset(PREFAB_URN, prefabData, Prefab.class);
        assertEquals(prefab, context.deserialize(context.serialize(prefab).build(), Prefab.class));
    }

}
