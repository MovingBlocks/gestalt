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

package org.terasology.assets.module;

import org.junit.Test;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextDeltaFormat;
import org.terasology.assets.test.stubs.text.TextFormat;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class ModuleAssetProducerTest extends VirtualModuleEnvironment {
    public static final String ASSET_TYPE_ID = "text";
    public static final String FOLDER_NAME = "text";
    public static final ResourceUrn URN = new ResourceUrn("test", "example");

    private ModuleAssetProducer<TextData> moduleProducer = new ModuleAssetProducer<>(ASSET_TYPE_ID, FOLDER_NAME);

    public ModuleAssetProducerTest() throws Exception {
        moduleProducer.addFormat(new TextFormat());
    }

    @Test
    public void resolveNoMatch() throws Exception {
        moduleProducer.setEnvironment(createEnvironment());

        Set<ResourceUrn> results = moduleProducer.resolve("madeUpThing", Name.EMPTY);
        assertTrue(results.isEmpty());
    }

    @Test
    public void resolveWithSingleMatch() throws Exception {
        moduleProducer.setEnvironment(createEnvironment());

        Set<ResourceUrn> results = moduleProducer.resolve(URN.getResourceName().toString(), Name.EMPTY);
        assertEquals(1, results.size());
        assertTrue(results.contains(URN));
    }

    @Test
    public void resolveWithMultipleMatchesFails() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")), moduleRegistry.getLatestModuleVersion(new Name("moduleA"))));

        Set<ResourceUrn> results = moduleProducer.resolve(URN.getResourceName().toString(), Name.EMPTY);
        assertEquals(2, results.size());
        assertTrue(results.contains(URN));
        assertTrue(results.contains(new ResourceUrn("moduleA", "example")));
    }

    @Test
    public void resolveInContext() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")), moduleRegistry.getLatestModuleVersion(new Name("moduleA"))));

        Set<ResourceUrn> results = moduleProducer.resolve(URN.getResourceName().toString(), new Name("moduleA"));
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertTrue(results.contains(new ResourceUrn("moduleA", "example")));
    }

    @Test
    public void resolveInContextDependency() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("moduleA")),
                moduleRegistry.getLatestModuleVersion(new Name("moduleB"))));

        Set<ResourceUrn> results = moduleProducer.resolve(URN.getResourceName().toString(), new Name("moduleA"));
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertTrue(results.contains(new ResourceUrn("moduleA", "example")));
    }

    @Test
    public void getMissingAsset() throws Exception {
        assertNull(moduleProducer.getAssetData(URN));
    }

    @Test
    public void loadAssetFromFile() throws Exception {
        moduleProducer.setEnvironment(createEnvironment());
        TextData assetData = moduleProducer.getAssetData(URN);
        assertNotNull(assetData);
        assertEquals("Example text", assetData.getValue());
    }

    @Test
    public void loadWithOverride() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA"))));

        TextData assetData = moduleProducer.getAssetData(URN);
        assertNotNull(assetData);
        assertEquals("Override text", assetData.getValue());
    }

    @Test
    public void loadWithOverrideInDependencyChain() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")), moduleRegistry.getLatestModuleVersion(new Name("overrideB"))));

        TextData assetData = moduleProducer.getAssetData(URN);
        assertNotNull(assetData);
        assertEquals("Different text", assetData.getValue());
    }

    @Test
    public void loadWithOverrideInUnrelatedModulesUsesAlphabeticallyLast() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")), moduleRegistry.getLatestModuleVersion(new Name("overrideC"))));

        TextData assetData = moduleProducer.getAssetData(URN);
        assertNotNull(assetData);
        assertEquals("Final text", assetData.getValue());
    }

    @Test
    public void loadWithDelta() throws Exception {
        moduleProducer.addDeltaFormat(new TextDeltaFormat());
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("deltaA"))));

        TextData assetData = moduleProducer.getAssetData(URN);
        assertNotNull(assetData);
        assertEquals("Example frumple", assetData.getValue());
    }

    @Test
    public void deltaDroppedBeforeOverride() throws Exception {
        moduleProducer.addDeltaFormat(new TextDeltaFormat());
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("deltaA")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideD"))));

        TextData assetData = moduleProducer.getAssetData(URN);
        assertNotNull(assetData);
        assertEquals("Overridden text without delta", assetData.getValue());
    }
}
