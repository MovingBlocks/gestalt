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

import org.junit.Test;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.AssetFactory;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.test.Return;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.assets.test.stubs.extensions.ExtensionAsset;
import org.terasology.assets.test.stubs.extensions.ExtensionDataProducer;
import org.terasology.assets.test.stubs.extensions.ExtensionDeltaFileFormat;
import org.terasology.assets.test.stubs.extensions.ExtensionFileFormat;
import org.terasology.assets.test.stubs.extensions.ExtensionSupplementalFileFormat;
import org.terasology.assets.test.stubs.inheritance.AlternateAsset;
import org.terasology.assets.test.stubs.inheritance.AlternateAssetData;
import org.terasology.assets.test.stubs.inheritance.ChildAsset;
import org.terasology.assets.test.stubs.inheritance.ChildAssetData;
import org.terasology.assets.test.stubs.inheritance.ParentAsset;
import org.terasology.assets.test.stubs.text.Text;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextFactory;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class ModuleAwareAssetTypeManagerTest extends VirtualModuleEnvironment {

    public static final String TEXT_VALUE = "Value";
    public static final String TEXT_VALUE_2 = "Value_2";

    public static final ResourceUrn URN = new ResourceUrn("test", "example");

    private ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManager();

    public ModuleAwareAssetTypeManagerTest() throws Exception {
    }

    @Test
    public void registerCoreAssetType() {
        TextFactory factory = new TextFactory();
        assetTypeManager.registerCoreAssetType(Text.class, factory);
        assertFalse(assetTypeManager.getAssetType(Text.class).isPresent());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        assertTrue(assetTypeManager.getAssetType(Text.class).isPresent());
        assertTrue(assetTypeManager.getAssetType(Text.class).get().getProducers().isEmpty());
    }

    @Test
    public void createCoreAssetTypeWithAssetFolder() {
        TextFactory factory = new TextFactory();
        assetTypeManager.registerCoreAssetType(Text.class, factory, "text");
        assertFalse(assetTypeManager.getAssetType(Text.class).isPresent());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        assertTrue(assetTypeManager.getAssetType(Text.class).get().getProducers().get(0) instanceof ModuleAssetDataProducer);
    }

    @Test
    public void setEnvironmentPopulatesModuleProducers() throws Exception {
        TextFactory factory = new TextFactory();
        assetTypeManager.registerCoreAssetType(Text.class, factory, "text");

        ModuleEnvironment environment = createEmptyEnvironment();
        assetTypeManager.switchEnvironment(environment);
        ModuleAssetDataProducer<?> moduleAssetDataProducer = (ModuleAssetDataProducer<?>) assetTypeManager.getAssetType(Text.class).get().getProducers().get(0);

        assertEquals(environment, moduleAssetDataProducer.getModuleEnvironment());
    }

    @Test
    public void disposeUnavailableAssetsOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        AssetDataProducer<TextData> producer = mock(AssetDataProducer.class);
        assetTypeManager.registerCoreProducer(Text.class, producer);
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(any(ResourceUrn.class))).thenReturn(Optional.empty());
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));
        assetTypeManager.switchEnvironment(createEmptyEnvironment());

        Optional<Text> asset = assetTypeManager.getAssetType(Text.class).get().getAsset(URN);
        assertTrue(asset.isPresent());
        assertFalse(asset.get().isDisposed());

        when(producer.getAssetData(URN)).thenReturn(Optional.empty());
        assetTypeManager.switchEnvironment(createEnvironment());
        assertTrue(asset.get().isDisposed());
    }

    @Test
    public void reloadAvailableAssetsOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());

        AssetDataProducer<TextData> producer = mock(AssetDataProducer.class);
        assetTypeManager.registerCoreProducer(Text.class, producer);
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));
        assetTypeManager.switchEnvironment(createEmptyEnvironment());

        Optional<? extends Text> asset = assetTypeManager.getAssetManager().getAsset(URN, Text.class);
        assertTrue(asset.isPresent());
        assertEquals(TEXT_VALUE, asset.get().getValue());

        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE_2)));
        assetTypeManager.switchEnvironment(createEnvironment());
        assertEquals(TEXT_VALUE_2, asset.get().getValue());
        assertFalse(asset.get().isDisposed());
    }

    @Test
    public void disposeAssetOnEnvironmentChangeIfRedirectExists() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());

        AssetDataProducer<TextData> producer = mock(AssetDataProducer.class);
        assetTypeManager.registerCoreProducer(Text.class, producer);
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));
        assetTypeManager.switchEnvironment(createEmptyEnvironment());

        Optional<? extends Text> asset = assetTypeManager.getAssetManager().getAsset(URN, Text.class);

        when(producer.redirect(URN)).thenReturn(new ResourceUrn(URN.getModuleName(), new Name("redirect")));
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        assertTrue(asset.get().isDisposed());
    }

    @Test
    public void removeAssetType() {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        assetTypeManager.removeCoreAssetType(Text.class);
        assertFalse(assetTypeManager.getAssetType(Text.class).isPresent());
    }

    @Test
    public void removedAssetTypeIsDisposed() {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        Text asset = assetTypeManager.getAssetManager().loadAsset(URN, new TextData(TEXT_VALUE), Text.class);
        assetTypeManager.removeCoreAssetType(Text.class);
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        assertTrue(asset.isDisposed());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionAssetType() throws Exception {
        assertFalse(assetTypeManager.getAssetType(ExtensionAsset.class).isPresent());
        assetTypeManager.switchEnvironment(createEnvironment());
        assertTrue(assetTypeManager.getAssetType(ExtensionAsset.class).isPresent());
    }

    @Test
    public void extensionAssetTypeRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.switchEnvironment(createEnvironment());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        assertFalse(assetTypeManager.getAssetType(ExtensionAsset.class).isPresent());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionProducers() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        assetTypeManager.switchEnvironment(createEnvironment());
        assertEquals(1, assetTypeManager.getAssetType(Text.class).get().getProducers().size());
        assertTrue(assetTypeManager.getAssetType(Text.class).get().getProducers().get(0) instanceof ExtensionDataProducer);
    }

    @Test
    public void extensionProducerRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        assetTypeManager.switchEnvironment(createEnvironment());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        assertTrue(assetTypeManager.getAssetType(Text.class).get().getProducers().isEmpty());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionFormats() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.switchEnvironment(createEnvironment());
        ModuleAssetDataProducer<TextData> moduleProducer = (ModuleAssetDataProducer<TextData>) assetTypeManager.getAssetType(Text.class).get().getProducers().get(0);
        assertEquals(1, moduleProducer.getAssetFormats().size());
        assertTrue(moduleProducer.getAssetFormats().get(0) instanceof ExtensionFileFormat);
    }

    @Test
    public void extensionFormatRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.switchEnvironment(createEnvironment());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        ModuleAssetDataProducer<TextData> moduleProducer = (ModuleAssetDataProducer<TextData>) assetTypeManager.getAssetType(Text.class).get().getProducers().get(0);
        assertTrue(moduleProducer.getAssetFormats().isEmpty());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionSupplementalFormats() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.switchEnvironment(createEnvironment());
        ModuleAssetDataProducer<TextData> moduleProducer = (ModuleAssetDataProducer<TextData>) assetTypeManager.getAssetType(Text.class).get().getProducers().get(0);
        assertEquals(1, moduleProducer.getSupplementFormats().size());
        assertTrue(moduleProducer.getSupplementFormats().get(0) instanceof ExtensionSupplementalFileFormat);
    }

    @Test
    public void extensionSupplementalFormatRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.switchEnvironment(createEnvironment());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        ModuleAssetDataProducer<TextData> moduleProducer = (ModuleAssetDataProducer<TextData>) assetTypeManager.getAssetType(Text.class).get().getProducers().get(0);
        assertTrue(moduleProducer.getSupplementFormats().isEmpty());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionDeltaFormats() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.switchEnvironment(createEnvironment());
        ModuleAssetDataProducer<TextData> moduleProducer = (ModuleAssetDataProducer<TextData>) assetTypeManager.getAssetType(Text.class).get().getProducers().get(0);
        assertEquals(1, moduleProducer.getDeltaFormats().size());
        assertTrue(moduleProducer.getDeltaFormats().get(0) instanceof ExtensionDeltaFileFormat);
    }

    @Test
    public void extensionDeltaFormatRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.switchEnvironment(createEnvironment());
        assetTypeManager.switchEnvironment(createEmptyEnvironment());
        ModuleAssetDataProducer<TextData> moduleProducer = (ModuleAssetDataProducer<TextData>) assetTypeManager.getAssetType(Text.class).get().getProducers().get(0);
        assertTrue(moduleProducer.getDeltaFormats().isEmpty());
    }

    @Test
    public void handleInheritanceRelationOfAssetTypes() {
        AssetFactory<ChildAsset, ChildAssetData> childAssetFactory = mock(AssetFactory.class);
        assetTypeManager.registerCoreAssetType(ChildAsset.class, childAssetFactory);
        AssetFactory<AlternateAsset, AlternateAssetData> alternativeAssetFactory = mock(AssetFactory.class);
        assetTypeManager.registerCoreAssetType(AlternateAsset.class, alternativeAssetFactory);
        assetTypeManager.switchEnvironment(createEmptyEnvironment());

        List<AssetType<? extends ParentAsset, ?>> assetTypes = assetTypeManager.getAssetTypes(ParentAsset.class);
        assertEquals(2, assetTypes.size());
        assertEquals(AlternateAsset.class, assetTypes.get(0).getAssetClass());
        assertEquals(ChildAsset.class, assetTypes.get(1).getAssetClass());
    }
}
