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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.AssetAlterationFileFormat;
import org.terasology.assets.format.AssetFileFormat;
import org.terasology.assets.format.producer.AssetFileDataProducer;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextDeltaFileFormat;
import org.terasology.assets.test.stubs.text.TextFileFormat;
import org.terasology.assets.test.stubs.text.TextMetadataFileFormat;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Immortius
 */
public class AssetFileDataProducerTest extends VirtualModuleEnvironment {
    private static final String FOLDER_NAME = "text";
    private static final ResourceUrn URN = new ResourceUrn("test", "example");

    private ModuleAssetScanner scanner = new ModuleAssetScanner();

    public AssetFileDataProducerTest() throws Exception {
    }


    private AssetFileDataProducer<TextData> createProducer(ModuleEnvironment environment) {
        ModuleEnvironmentDependencyProvider dependencyProvider = new ModuleEnvironmentDependencyProvider(environment);
        AssetFileDataProducer<TextData> textDataModuleAssetDataProducer = new AssetFileDataProducer<>(dependencyProvider, FOLDER_NAME);
        textDataModuleAssetDataProducer.addAssetFormat(new TextFileFormat());
        scanner.scan(environment, textDataModuleAssetDataProducer);
        return textDataModuleAssetDataProducer;
    }

    @Test
    public void getModulesProvidingWithNoMatch() throws Exception {
        Set<Name> results = createProducer(createEnvironment()).getModulesProviding(new Name("madeUpThing"));
        assertTrue(results.isEmpty());
    }

    @Test
    public void getModulesProvidingWithSingleMatch() throws Exception {
        Set<Name> results = createProducer(createEnvironment()).getModulesProviding(URN.getResourceName());
        assertEquals(1, results.size());
        assertTrue(results.contains(URN.getModuleName()));
    }

    @Test
    public void resolveWithMultipleMatches() throws Exception {
        AssetFileDataProducer<TextData> producer = createProducer(createEnvironment(
                moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("moduleA"))));

        Set<Name> results = producer.getModulesProviding(URN.getResourceName());
        assertEquals(2, results.size());
        assertTrue(results.contains(URN.getModuleName()));
        assertTrue(results.contains(new Name("moduleA")));
    }

    @Test
    public void getMissingAsset() throws Exception {
        assertFalse(createProducer(createEmptyEnvironment()).getAssetData(URN).isPresent());
    }

    @Test
    public void loadAssetFromFile() throws Exception {
        Optional<TextData> assetData = createProducer(createEnvironment()).getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Example text", assetData.get().getValue());
    }

    @Test
    public void loadWithOverride() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Override text", assetData.get().getValue());
    }

    @Test
    public void ignoreOverrideInDifferentTypeFolder() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideE"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Example text", assetData.get().getValue());
    }

    @Test
    public void loadWithOverrideInDependencyChain() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")), moduleRegistry.getLatestModuleVersion(new Name("overrideB"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Different text", assetData.get().getValue());
    }

    @Test
    public void loadWithOverrideInUnrelatedModulesUsesAlphabeticallyLast() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")), moduleRegistry.getLatestModuleVersion(new Name("overrideC"))));

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Override text", assetData.get().getValue());
    }

    @Test
    public void loadWithDelta() throws Exception {
        ModuleEnvironment environment = createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")), moduleRegistry.getLatestModuleVersion(new Name("deltaA")));
        AssetFileDataProducer<TextData> moduleProducer = createProducer(environment);
        moduleProducer.addDeltaFormat(new TextDeltaFileFormat());
        scanner.scan(environment, moduleProducer);

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Example frumple", assetData.get().getValue());
    }

    @Test
    public void loadWithDeltaUnrelatedToOverride() throws Exception {
        ModuleEnvironment environment = createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideA")),
                moduleRegistry.getLatestModuleVersion(new Name("deltaA")));
        AssetFileDataProducer<TextData> moduleProducer = createProducer(environment);
        moduleProducer.addDeltaFormat(new TextDeltaFileFormat());
        scanner.scan(environment, moduleProducer);

        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Override frumple", assetData.get().getValue());
    }

    @Test
    public void deltaDroppedBeforeOverride() throws Exception {
        ModuleEnvironment environment = createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("deltaA")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideD")));
        AssetFileDataProducer<TextData> moduleProducer = createProducer(environment);
        moduleProducer.addDeltaFormat(new TextDeltaFileFormat());
        scanner.scan(environment, moduleProducer);


        Optional<TextData> assetData = moduleProducer.getAssetData(URN);
        assertTrue(assetData.isPresent());
        assertEquals("Overridden text without delta", assetData.get().getValue());
    }

    @Test
    public void redirects() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("redirectA"))));
        assertEquals(new ResourceUrn("redirectA:real"), moduleProducer.redirect(new ResourceUrn("redirectA:example")));
    }

    @Test
    public void chainedRedirects() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("redirectA"))));
        assertEquals(new ResourceUrn("redirectA:real"), moduleProducer.redirect(new ResourceUrn("redirectA:double")));
    }

    @Test
    public void handleRedirectResolution() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("redirectA"))));

        Set<Name> results = moduleProducer.getModulesProviding(new Name("example"));
        assertEquals(1, results.size());
        assertTrue(results.contains(new Name("redirectA")));
    }

    @Test
    public void applySupplements() throws Exception {
        ModuleEnvironment environment = createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("supplementA")));
        AssetFileDataProducer<TextData> moduleProducer = createProducer(environment);
        moduleProducer.addSupplementFormat(new TextMetadataFileFormat());
        scanner.scan(environment, moduleProducer);

        Optional<TextData> data = moduleProducer.getAssetData(new ResourceUrn("supplementA:example"));
        assertTrue(data.isPresent());
        assertEquals("bold", data.get().getMetadata());
    }

    @Test
    public void overrideWithSupplement() throws Exception {
        ModuleEnvironment environment = createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("supplementA")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideSupplement")));
        AssetFileDataProducer<TextData> moduleProducer = createProducer(environment);
        moduleProducer.addSupplementFormat(new TextMetadataFileFormat());
        scanner.scan(environment, moduleProducer);

        Optional<TextData> data = moduleProducer.getAssetData(new ResourceUrn("supplementA:example"));
        assertTrue(data.isPresent());
        assertEquals("sweet", data.get().getMetadata());
    }

    @Test
    public void orphanOverrideSupplementIgnored() throws Exception {
        AssetFileDataProducer<TextData> moduleProducer = createProducer(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("moduleA")),
                moduleRegistry.getLatestModuleVersion(new Name("overrideWithSupplementOnly"))));

        Optional<TextData> data = moduleProducer.getAssetData(new ResourceUrn("moduleA:example"));
        assertTrue(data.isPresent());
        assertEquals("", data.get().getMetadata());
    }

}
