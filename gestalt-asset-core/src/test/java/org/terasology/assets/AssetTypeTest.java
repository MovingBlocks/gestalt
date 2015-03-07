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

package org.terasology.assets;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.terasology.assets.test.Return;
import org.terasology.assets.test.VirtualModuleEnvironment;
import org.terasology.assets.test.stubs.text.Text;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextFactory;
import org.terasology.naming.Name;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class AssetTypeTest extends VirtualModuleEnvironment {

    public static final String TEXT_VALUE = "Value";
    public static final String TEXT_VALUE_2 = "Value_2";

    public static final ResourceUrn URN = new ResourceUrn("test", "example");

    private AssetType<Text, TextData> assetType = new AssetType<>(Text.class);

    public AssetTypeTest() throws Exception {
        assetType.setFactory(new TextFactory());
    }

    @Test
    public void construction() {
        assertEquals(Text.class, assetType.getAssetClass());
        assertTrue(assetType.getProducers().isEmpty());
    }

    @Test
    public void loadData() {
        TextData data = new TextData(TEXT_VALUE);
        Text text = new Text(URN, data);

        assertFalse(assetType.isLoaded(URN));
        Text createdText = assetType.loadAsset(URN, data);
        assertEquals(text, createdText);
        assertTrue(assetType.isLoaded(URN));
    }

    @Test
    public void retrieveLoadedDataByUrn() {
        TextData data = new TextData(TEXT_VALUE);

        Text loadedText = assetType.loadAsset(URN, data);
        Optional<Text> retrievedText = assetType.getAsset(URN);
        assertEquals(loadedText, retrievedText.get());
    }

    @Test
    public void loadingAssetWithSameUrnReloadsExistingAsset() {
        TextData initialData = new TextData(TEXT_VALUE);
        Text initialText = assetType.loadAsset(URN, initialData);
        TextData newData = new TextData(TEXT_VALUE_2);

        Text newText = assetType.loadAsset(URN, newData);
        assertSame(initialText, newText);
        assertEquals(newData.getValue(), initialText.getValue());
    }

    @Test
    public void changingFactoryDisposesAllAssets() {
        TextData data = new TextData(TEXT_VALUE);
        Text asset = assetType.loadAsset(URN, data);

        assetType.setFactory(mock(AssetFactory.class));
        assertTrue(asset.isDisposed());
        assertFalse(assetType.getAsset(URN).isPresent());
    }

    @Test
    public void disposingAsset() {
        TextData data = new TextData(TEXT_VALUE);
        Text asset = assetType.loadAsset(URN, data);

        asset.dispose();
        assertTrue(asset.isDisposed());
        assertFalse(assetType.getAsset(URN).isPresent());
    }

    @Test
    public void addProducer() {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        assertEquals(1, assetType.getProducers().size());
        assertTrue(assetType.getProducers().contains(producer));
    }

    @Test
    public void removeProducer() {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        assetType.removeProducer(producer);
        assertTrue(assetType.getProducers().isEmpty());
    }

    @Test
    public void clearProducers() {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        assetType.clearProducers();
        assertTrue(assetType.getProducers().isEmpty());
    }

    @Test
    public void resolveFullUrnReturnsUrn() {
        Set<ResourceUrn> results = assetType.resolve(URN.toString());
        assertEquals(1, results.size());
        assertTrue(results.contains(URN));
    }

    @Test
    public void resolvePartialReturnsNothingWithNoPossibilities() {
        Set<ResourceUrn> results = assetType.resolve(URN.getResourceName().toString());
        assertTrue(results.isEmpty());
    }

    @Test
    public void resolvePartialReturnsPossibilitiesFromProducers() {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.resolve(URN.getResourceName().toString(), Name.EMPTY)).thenReturn(ImmutableSet.of(URN));
        Set<ResourceUrn> results = assetType.resolve(URN.getResourceName().toString());
        assertEquals(1, results.size());
        assertTrue(results.contains(URN));
    }

    @Test
    public void resolvePartialWithContextPassesContextToProducers() {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.resolve(URN.getResourceName().toString(), URN.getModuleName())).thenReturn(ImmutableSet.of(URN));
        Set<ResourceUrn> results = assetType.resolve(URN.getResourceName().toString(), URN.getModuleName());
        assertEquals(1, results.size());
        assertTrue(results.contains(URN));
    }

    @Test
    public void getUnknownAsset() {
        assertFalse(assetType.getAsset(URN).isPresent());
    }

    @Test
    public void getAssetLoadsFromProducers() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));

        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertEquals(URN, asset.get().getUrn());
        assertEquals(TEXT_VALUE, asset.get().getValue());
        assertTrue(assetType.isLoaded(URN));
    }

    @Test
    public void getAssetWhenProducerFails() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer.getAssetData(URN)).thenThrow(new IOException());

        assertFalse(assetType.getAsset(URN).isPresent());
        assertFalse(assetType.isLoaded(URN));
    }

    @Test
    public void followRedirectsGettingAssets() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        ResourceUrn realUrn = new ResourceUrn("engine:real");
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer.redirect(URN)).thenReturn(realUrn);
        when(producer.getAssetData(realUrn)).thenReturn(Optional.of(new TextData(TEXT_VALUE_2)));
        assetType.addProducer(producer);

        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertEquals(realUrn, asset.get().getUrn());
        assertEquals(TEXT_VALUE_2, asset.get().getValue());
    }

    @Test
    public void redirectsChainForMultipleProducers() throws Exception {
        ResourceUrn realUrn = new ResourceUrn("engine:real");
        ResourceUrn realUrn2 = new ResourceUrn("engine:real2");

        AssetDataProducer producer = mock(AssetDataProducer.class);
        when(producer.getAssetData(any(ResourceUrn.class))).thenReturn(Optional.absent());
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer.redirect(URN)).thenReturn(realUrn);

        AssetDataProducer producer2 = mock(AssetDataProducer.class);
        when(producer2.getAssetData(any(ResourceUrn.class))).thenReturn(Optional.absent());
        when(producer2.getAssetData(realUrn2)).thenReturn(Optional.of(new TextData(TEXT_VALUE_2)));
        when(producer2.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer2.redirect(realUrn)).thenReturn(realUrn2);

        assetType.addProducer(producer);
        assetType.addProducer(producer2);

        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertEquals(realUrn2, asset.get().getUrn());
        assertEquals(TEXT_VALUE_2, asset.get().getValue());
    }

    @Test
    public void redirectsChainForMultipleProducersAnyOrder() throws Exception {
        ResourceUrn realUrn = new ResourceUrn("engine:real");
        ResourceUrn realUrn2 = new ResourceUrn("engine:real2");

        AssetDataProducer producer = mock(AssetDataProducer.class);
        when(producer.getAssetData(any(ResourceUrn.class))).thenReturn(Optional.absent());
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer.redirect(URN)).thenReturn(realUrn);

        AssetDataProducer producer2 = mock(AssetDataProducer.class);
        when(producer2.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer2.redirect(realUrn)).thenReturn(realUrn2);
        when(producer2.getAssetData(realUrn2)).thenReturn(Optional.of(new TextData(TEXT_VALUE_2)));

        assetType.addProducer(producer2);
        assetType.addProducer(producer);

        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertEquals(realUrn2, asset.get().getUrn());
        assertEquals(TEXT_VALUE_2, asset.get().getValue());
    }

    @Test
    public void disposeAssetsOnDisposeAll() throws Exception {
        TextData data = new TextData(TEXT_VALUE);
        Text createdText = assetType.loadAsset(URN, data);

        assetType.disposeAll();
        assertTrue(createdText.isDisposed());
        assertFalse(assetType.getAsset(URN).isPresent());
    }

    @Test
    public void disposeUnavailableAssetsOnRefresh() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(any(ResourceUrn.class))).thenReturn(Optional.absent());
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));

        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertFalse(asset.get().isDisposed());

        when(producer.getAssetData(URN)).thenReturn(Optional.absent());
        assetType.refresh();
        assertTrue(asset.get().isDisposed());
    }

    @Test
    public void reloadAvailableAssetsOnRefresh() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));

        Optional<Text> asset = assetType.getAsset(URN);
        assertTrue(asset.isPresent());
        assertEquals(TEXT_VALUE, asset.get().getValue());

        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE_2)));
        assetType.refresh();
        assertEquals(TEXT_VALUE_2, asset.get().getValue());
        assertFalse(asset.get().isDisposed());
    }

    @Test
    public void disposeAssetOnRefreshIfRedirectExists() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        when(producer.redirect(URN)).thenReturn(URN);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(new TextData(TEXT_VALUE)));
        Optional<Text> asset = assetType.getAsset(URN);
        when(producer.redirect(URN)).thenReturn(new ResourceUrn(URN.getModuleName(), new Name("redirect")));

        assetType.refresh();
        assertTrue(asset.get().isDisposed());
    }

    @Test
    public void loadAssetInstance() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        TextData data = new TextData(TEXT_VALUE);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(data));
        AssetFactory factory = mock(AssetFactory.class);
        assetType.setFactory(factory);
        Text textAsset = new Text(URN, data);
        when(factory.build(URN, data)).thenReturn(textAsset);

        Optional<Text> result = assetType.getAsset(URN.getInstanceUrn());
        assertTrue(result.isPresent());
        assertNotSame(textAsset, result.get());
        assertTrue(result.get().getUrn().isInstance());
        assertEquals(URN, result.get().getUrn().getParentUrn());
    }

    @Test
    public void disposingParentDisposesChildAsset() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.redirect(any(ResourceUrn.class))).thenAnswer(Return.firstArgument());
        TextData data = new TextData(TEXT_VALUE);
        when(producer.getAssetData(URN)).thenReturn(Optional.of(data));
        AssetFactory factory = mock(AssetFactory.class);
        assetType.setFactory(factory);
        Text textAsset = new Text(URN, data);
        when(factory.build(URN, data)).thenReturn(textAsset);

        Optional<Text> parent = assetType.getAsset(URN);
        Text result = assetType.createInstance(parent.get());

        assertNotSame(textAsset, result);
        assertTrue(result.getUrn().isInstance());
        assertEquals(URN, result.getUrn().getParentUrn());
    }

    @Test
    public void resolvePartialInstanceUrn() throws Exception {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        assetType.addProducer(producer);
        when(producer.resolve(URN.getResourceName().toString(), Name.EMPTY)).thenReturn(ImmutableSet.of(URN));
        Set<ResourceUrn> results = assetType.resolve(URN.getResourceName().toString() + ResourceUrn.INSTANCE_INDICATOR);
        assertEquals(1, results.size());
        assertTrue(results.contains(URN.getInstanceUrn()));
    }

    @Test
    public void getLoaded() throws Exception {
        TextData data = new TextData(TEXT_VALUE);

        assertTrue(assetType.getLoadedAssetUrns().isEmpty());
        assetType.loadAsset(URN, data);
        assertEquals(ImmutableSet.of(URN), assetType.getLoadedAssetUrns());
    }

    @Test
    public void getAvailableIncludesLoaded() {
        TextData data = new TextData(TEXT_VALUE);

        assertTrue(assetType.getAvailableAssetUrns().isEmpty());
        assetType.loadAsset(URN, data);
        assertEquals(ImmutableSet.of(URN), assetType.getAvailableAssetUrns());
    }

    @Test
    public void getAvailableIncludesFromProducer() {
        AssetDataProducer producer = mock(AssetDataProducer.class);
        when(producer.getAvailableAssetUrns()).thenReturn(ImmutableSet.of(URN));
        assetType.addProducer(producer);

        assertEquals(ImmutableSet.of(URN), assetType.getAvailableAssetUrns());
    }

}
