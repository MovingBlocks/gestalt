/*
 * Copyright 2014 MovingBlocks
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

package org.terasology.assets;

import org.junit.Before;
import org.junit.Test;
import org.terasology.assets.stubs.TestAssetTypes;
import org.terasology.module.ClasspathModule;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModuleMetadataReader;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.ModuleSecurityManager;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class AssetManagerTest {

    private AssetManager assetManager = new AssetManager();
    private ModuleEnvironment environment;

    @Before
    public void setup() throws Exception {

        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/module.info"))) {
            ModuleMetadata metadata = new ModuleMetadataReader().read(reader);
            Module module = ClasspathModule.create(metadata, getClass());
            ModuleSecurityManager securityManager = new ModuleSecurityManager();
            environment = new ModuleEnvironment(Arrays.asList(module), securityManager, Collections.<BytecodeInjector>emptyList());
        }
    }

    @Test
    public void initialState() {
        assertTrue(assetManager.getAssetTypes().isEmpty());
    }

    @Test
    public void registerAssetTypeAppearsInTypes() {
        AssetType type = mock(AssetType.class);
        when(type.getId()).thenReturn("test");
        assetManager.registerAssetType(type, "test");

        assertThat(assetManager.getAssetTypes().contains(type), is(true));
    }

    @Test
    public void unregisterAssetTypeRemovedFromTypes() {
        AssetType type = mock(AssetType.class);
        when(type.getId()).thenReturn("test");
        assetManager.registerAssetType(type, "test");

        assetManager.unregister(type);
        assertTrue(assetManager.getAssetTypes().isEmpty());
    }

    @Test
    public void registerAssetFormatCausesAssetDiscovery() {
        assetManager.setEnvironment(environment);
        assetManager.registerAssetType(TestAssetTypes.BOOK, "Books");
        assetManager.registerAssetFormat(TestAssetTypes.BOOK, );
    }


}
