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
import org.terasology.assets.AssetData;
import org.terasology.assets.AssetFactory;
import org.terasology.assets.AssetDataProducer;
import org.terasology.assets.AssetType;
import org.terasology.assets.test.Return;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.assets.test.stubs.extensions.ExtensionAsset;
import org.terasology.assets.test.stubs.extensions.ExtensionDeltaFileFormat;
import org.terasology.assets.test.stubs.extensions.ExtensionFileFormat;
import org.terasology.assets.test.stubs.extensions.ExtensionDataProducer;
import org.terasology.assets.test.stubs.extensions.ExtensionSupplementalFileFormat;
import org.terasology.assets.test.stubs.inheritance.AlternateAsset;
import org.terasology.assets.test.stubs.inheritance.ChildAsset;
import org.terasology.assets.test.stubs.inheritance.ParentAsset;
import org.terasology.assets.test.stubs.text.Text;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextFactory;
import org.terasology.module.ModuleEnvironment;
import org.terasology.naming.Name;
import org.terasology.assets.ResourceUrn;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    private ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManager(createEmptyEnvironment());

    public ModuleAwareAssetTypeManagerTest() throws Exception {
    }

    @Test
    public void createCoreAssetType() {
        TextFactory factory = new TextFactory();
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, factory);
        assertTrue(assetType.getProducers().isEmpty());
    }

    @Test
    public void createCoreAssetTypeWithAssetFolder() {
        TextFactory factory = new TextFactory();
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, factory, "text");
        assertEquals(1, assetType.getProducers().size());
        assertTrue(assetType.getProducers().get(0) instanceof ModuleAssetDataProducer);
    }

    @Test
    public void getModuleAssetProducerForFileBasedAssetType() {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        Optional<ModuleAssetDataProducer<TextData>> moduleProducer = assetTypeManager.getModuleProducerFor(Text.class);
        assertTrue(moduleProducer.isPresent());
    }

    @Test
    public void getModuleAssetProducerForFolderlessAssetType() {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        Optional<ModuleAssetDataProducer<TextData>> moduleProducer = assetTypeManager.getModuleProducerFor(Text.class);
        assertFalse(moduleProducer.isPresent());
    }

    @Test
    public void setEnvironmentPopulatesModuleProducers() throws Exception {
        TextFactory factory = new TextFactory();
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, factory, "text");
        ModuleAssetDataProducer<TextData> producer = (ModuleAssetDataProducer<TextData>) assetType.getProducers().get(0);

        ModuleEnvironment environment = createEnvironment();
        assertFalse(environment.equals(producer.getModuleEnvironment()));
        assetTypeManager.setEnvironment(environment);
        assertEquals(environment, assetTypeManager.getEnvironment());
        assertEquals(environment, producer.getModuleEnvironment());
    }

    @Test
    public void setUpNewTypesWithLastSetEnvironment() throws Exception {
        ModuleEnvironment environment = createEnvironment();
        assetTypeManager.setEnvironment(environment);

        TextFactory factory = new TextFactory();
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, factory, "text");
        ModuleAssetDataProducer<TextData> producer = (ModuleAssetDataProducer<TextData>) assetType.getProducers().get(0);
        assertEquals(environment, producer.getModuleEnvironment());
    }

    @Test
    public void disposeUnavailableAssetsOnEnvironmentChange() throws Exception {
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());

        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(any(ResourceUrn.class))).thenReturn(Optional.absent());
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));


        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertFalse(asset.get().isDisposed());

        when(producer.getAssetData(URN)).thenReturn(Optional.absent());
        assetTypeManager.setEnvironment(createEnvironment());
        assertTrue(asset.get().isDisposed());
    }

    @Test
    public void reloadAvailableAssetsOnEnvironmentChange() throws Exception {
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());

        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));

        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertEquals(TEXT_VALUE, asset.get().getValue());

        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE_2)));
        assetTypeManager.setEnvironment(createEnvironment());
        assertEquals(TEXT_VALUE_2, asset.get().getValue());
        assertFalse(asset.get().isDisposed());
    }

    @Test
    public void disposeAssetOnEnvironmentChangeIfRedirectExists() throws Exception {
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());

        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));
        Optional<Text> asset = assetType.getAsset(URN);
        when(producer.redirect(URN)).thenReturn(new ResourceUrn(URN.getModuleName(), new Name("redirect")));

        assetTypeManager.setEnvironment(createEnvironment());
        assertTrue(asset.get().isDisposed());
    }

    @Test
    public void removeAssetType() {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        assetTypeManager.removeCoreAssetType(Text.class);
        assertNull(assetTypeManager.getAssetType(Text.class));
    }

    @Test
    public void removedAssetTypeIsDisposed() {
        AssetType<Text, TextData> assetType = assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        Text asset = assetType.loadAsset(URN, new TextData(TEXT_VALUE));
        assetTypeManager.removeCoreAssetType(Text.class);
        assertTrue(asset.isDisposed());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionAssetType() throws Exception {
        assertNull(assetTypeManager.getAssetType(ExtensionAsset.class));
        assetTypeManager.setEnvironment(createEnvironment());
        assertNotNull(assetTypeManager.getAssetType(ExtensionAsset.class));
    }

    @Test
    public void extensionAssetTypeRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.setEnvironment(createEnvironment());
        assetTypeManager.setEnvironment(createEmptyEnvironment());
        assertNull(assetTypeManager.getAssetType(ExtensionAsset.class));
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionProducers() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        assetTypeManager.setEnvironment(createEnvironment());
        assertEquals(1, assetTypeManager.getAssetType(Text.class).getProducers().size());
        assertTrue(assetTypeManager.getAssetType(Text.class).getProducers().get(0) instanceof ExtensionDataProducer);
    }

    @Test
    public void extensionProducerRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory());
        assetTypeManager.setEnvironment(createEnvironment());
        assetTypeManager.setEnvironment(createEmptyEnvironment());
        assertTrue(assetTypeManager.getAssetType(Text.class).getProducers().isEmpty());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionFormats() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.setEnvironment(createEnvironment());
        assertEquals(1, assetTypeManager.getModuleProducerFor(Text.class).get().getAssetFormats().size());
        assertTrue(assetTypeManager.getModuleProducerFor(Text.class).get().getAssetFormats().get(0) instanceof ExtensionFileFormat);
    }

    @Test
    public void extensionFormatRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.setEnvironment(createEnvironment());
        assetTypeManager.setEnvironment(createEmptyEnvironment());
        assertTrue(assetTypeManager.getModuleProducerFor(Text.class).get().getAssetFormats().isEmpty());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionSupplementalFormats() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.setEnvironment(createEnvironment());
        assertEquals(1, assetTypeManager.getModuleProducerFor(Text.class).get().getSupplementFormats().size());
        assertTrue(assetTypeManager.getModuleProducerFor(Text.class).get().getSupplementFormats().get(0) instanceof ExtensionSupplementalFileFormat);
    }

    @Test
    public void extensionSupplementalFormatRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.setEnvironment(createEnvironment());
        assetTypeManager.setEnvironment(createEmptyEnvironment());
        assertTrue(assetTypeManager.getModuleProducerFor(Text.class).get().getSupplementFormats().isEmpty());
    }

    @Test
    public void setEnvironmentTriggersLoadOfExtensionDeltaFormats() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.setEnvironment(createEnvironment());
        assertEquals(1, assetTypeManager.getModuleProducerFor(Text.class).get().getDeltaFormats().size());
        assertTrue(assetTypeManager.getModuleProducerFor(Text.class).get().getDeltaFormats().get(0) instanceof ExtensionDeltaFileFormat);
    }

    @Test
    public void extensionDeltaFormatRemovedOnEnvironmentChange() throws Exception {
        assetTypeManager.registerCoreAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.setEnvironment(createEnvironment());
        assetTypeManager.setEnvironment(createEmptyEnvironment());
        assertTrue(assetTypeManager.getModuleProducerFor(Text.class).get().getDeltaFormats().isEmpty());
    }

    @Test
    public void handleInheritanceRelationOfAssetTypes() {
        assetTypeManager.registerCoreAssetType(ChildAsset.class, mock(AssetFactory.class));
        assetTypeManager.registerCoreAssetType(AlternateAsset.class, mock(AssetFactory.class));

        List<AssetType<? extends ParentAsset, ? extends AssetData>> assetTypes = assetTypeManager.getAssetTypes(ParentAsset.class);
        assertEquals(2, assetTypes.size());
        assertEquals(AlternateAsset.class, assetTypes.get(0).getAssetClass());
        assertEquals(ChildAsset.class, assetTypes.get(1).getAssetClass());
    }
}
