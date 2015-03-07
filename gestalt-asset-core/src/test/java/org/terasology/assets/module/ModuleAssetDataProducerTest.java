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

package org.terasology.assets.module;

import com.google.common.base.Optional;
import org.junit.Test;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextDeltaFileFormat;
import org.terasology.assets.test.stubs.text.TextFileFormat;
import org.terasology.assets.test.stubs.text.TextMetadataFileFormat;
import org.terasology.naming.Name;
import org.terasology.assets.ResourceUrn;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class ModuleAssetDataProducerTest extends VirtualModuleEnvironment {
    public static final String FOLDER_NAME = "text";
    public static final ResourceUrn URN = new ResourceUrn("test", "example");

    private ModuleAssetDataProducer<TextData> moduleProducer = new ModuleAssetDataProducer<>(FOLDER_NAME);

    public ModuleAssetDataProducerTest() throws Exception {
        moduleProducer.addAssetFormat(new TextFileFormat());
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
        assertFalse(moduleProducer.getAssetData(URN).isPresent());
    }

    @Test
    public void loadAssetFromFile() throws Exception {
        moduleProducer.setEnvironment(createEnvironment());
        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Example text", assetData.get().getValue());
    }

    @Test
    public void loadWithOverride() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Override text", assetData.get().getValue());
    }

    @Test
    public void loadWithOverrideInDependencyChain() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")), moduleRegistry.getLatestModuleVersion(new Name("overrideB"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Different text", assetData.get().getValue());
    }

    @Test
    public void loadWithOverrideInUnrelatedModulesUsesAlphabeticallyLast() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")), moduleRegistry.getLatestModuleVersion(new Name("overrideC"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Override text", assetData.get().getValue());
    }

    @Test
    public void loadWithDelta() throws Exception {
        moduleProducer.addDeltaFormat(new TextDeltaFileFormat());
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("deltaA"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Example frumple", assetData.get().getValue());
    }

    @Test
    public void loadWithDeltaUnrelatedToOverride() throws Exception {
        moduleProducer.addDeltaFormat(new TextDeltaFileFormat());
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")),
                moduleRegistry.getLatestModuleVersion(new Name("deltaA"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Override frumple", assetData.get().getValue());
    }

    @Test
    public void deltaDroppedBeforeOverride() throws Exception {
        moduleProducer.addDeltaFormat(new TextDeltaFileFormat());
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("deltaA")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideD"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Overridden text without delta", assetData.get().getValue());
    }

    @Test
    public void redirects() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("redirectA"))));
        assertEquals(new ResourceUrn("redirectA:real"), moduleProducer.redirect(new ResourceUrn("redirectA:example")));
    }

    @Test
    public void chainedRedirects() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("redirectA"))));
        assertEquals(new ResourceUrn("redirectA:real"), moduleProducer.redirect(new ResourceUrn("redirectA:double")));
    }

    @Test
    public void handleRedirectResolution() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("redirectA"))));

        Set<ResourceUrn> results = moduleProducer.resolve("example", Name.EMPTY);
        assertEquals(1, results.size());
        assertTrue(results.contains(new ResourceUrn("redirectA:example")));
    }

    @Test
    public void applySupplements() throws Exception {
        moduleProducer.addSupplementFormat(new TextMetadataFileFormat());
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("supplementA"))));

        Optional<TextData> data = moduleProducer.getAssetData(new ResourceUrn("supplementA:example"));
        assertTrue(data.isPresent());
        assertEquals("bold", data.get().getMetadata());
    }

    @Test
    public void overrideWithSupplement() throws Exception {
        moduleProducer.addSupplementFormat(new TextMetadataFileFormat());
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("supplementA")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideSupplement"))));

        Optional<TextData> data = moduleProducer.getAssetData(new ResourceUrn("supplementA:example"));
        assertTrue(data.isPresent());
        assertEquals("sweet", data.get().getMetadata());
    }

    @Test
    public void resolvePartialInstanceUrns() throws Exception {
        moduleProducer.setEnvironment(createEnvironment());

        Set<ResourceUrn> results = moduleProducer.resolve(URN.getResourceName().toString() + ResourceUrn.INSTANCE_INDICATOR, Name.EMPTY);
        assertEquals(1, results.size());
        assertTrue(results.contains(URN.getInstanceUrn()));
    }

    @Test
    public void orphanOverrideSupplementIgnored() throws Exception {
        moduleProducer.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("moduleA")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideWithSupplementOnly"))));

        Optional<TextData> data = moduleProducer.getAssetData(new ResourceUrn("moduleA:example"));
        assertTrue(data.isPresent());
        assertEquals("", data.get().getMetadata());
    }

}
