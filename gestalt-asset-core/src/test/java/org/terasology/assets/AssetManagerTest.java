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

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.management.Context;
import org.terasology.assets.management.ContextManager;
import org.terasology.assets.management.MapAssetTypeManager;
import org.terasology.assets.test.OptionalAnswer;
import org.terasology.assets.test.Return;
import org.terasology.assets.test.stubs.inheritance.AlternateAsset;
import org.terasology.assets.test.stubs.inheritance.AlternateAssetData;
import org.terasology.assets.test.stubs.inheritance.AlternateAssetFactory;
import org.terasology.assets.test.stubs.inheritance.ChildAsset;
import org.terasology.assets.test.stubs.inheritance.ChildAssetData;
import org.terasology.assets.test.stubs.inheritance.ChildAssetFactory;
import org.terasology.assets.test.stubs.inheritance.ParentAsset;
import org.terasology.assets.test.stubs.text.Text;
import org.terasology.assets.test.stubs.text.TextData;
import org.terasology.assets.test.stubs.text.TextFactory;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class AssetManagerTest {

    private static final ResourceUrn ENGINE_TEST_URN = new ResourceUrn("engine", "test");
    private static final ResourceUrn ENGINE_TEST2_URN = new ResourceUrn("engine", "test2");
    private static final ResourceUrn MORE_TEST_URN = new ResourceUrn("more", "test");

    private MapAssetTypeManager assetTypeManager = new MapAssetTypeManager();
    private AssetManager assetManager = new AssetManager(assetTypeManager);
    private AssetType<Text, TextData> textAssetType = assetTypeManager.createAssetType(Text.class, new TextFactory());
    private AssetType<ChildAsset, ChildAssetData> childAssetType = assetTypeManager.createAssetType(ChildAsset.class, new ChildAssetFactory());
    private AssetType<AlternateAsset, AlternateAssetData> alternateAssetType = assetTypeManager.createAssetType(AlternateAsset.class, new AlternateAssetFactory());

    @Test
    public void getAvailableAssets() {
        assertTrue(assetManager.getAvailableAssets(Text.class).isEmpty());

        AssetDataProducer<TextData> producer = mock(AssetDataProducer.class);
        when(producer.getAvailableAssetUrns()).thenReturn(ImmutableSet.of(ENGINE_TEST_URN));
        textAssetType.addProducer(producer);

        assertEquals(ImmutableSet.of(ENGINE_TEST_URN), assetManager.getAvailableAssets(Text.class));
    }

    @Test
    public void getAvailableAssetsCombinesInheritedAssets() {
        AssetDataProducer<ChildAssetData> childProducer = mock(AssetDataProducer.class);
        when(childProducer.getAvailableAssetUrns()).thenReturn(ImmutableSet.of(ENGINE_TEST_URN));
        childAssetType.addProducer(childProducer);

        AssetDataProducer<AlternateAssetData> alternateProducer = mock(AssetDataProducer.class);
        when(alternateProducer.getAvailableAssetUrns()).thenReturn(ImmutableSet.of(ENGINE_TEST2_URN));
        alternateAssetType.addProducer(alternateProducer);

        assertEquals(ImmutableSet.of(ENGINE_TEST_URN, ENGINE_TEST2_URN), assetManager.getAvailableAssets(ParentAsset.class));
    }

    @Test
    public void checkIfAssetIsLoaded() {
        textAssetType.loadAsset(ENGINE_TEST_URN, new TextData("moo"));
        assertFalse(assetManager.isLoaded(MORE_TEST_URN, Text.class));
        assertFalse(assetManager.isLoaded(ENGINE_TEST_URN, AlternateAsset.class));
        assertTrue(assetManager.isLoaded(ENGINE_TEST_URN, Text.class));
    }

    @Test
    public void getLoadedAssetUrns() {
        textAssetType.loadAsset(ENGINE_TEST_URN, new TextData("moo"));
        assertEquals(ImmutableSet.of(ENGINE_TEST_URN), assetManager.getLoadedAssetUrns(Text.class));
    }

    @Test
    public void getLoadedAssetUrnsCombinesInherited() {
        childAssetType.loadAsset(ENGINE_TEST_URN, new ChildAssetData());
        alternateAssetType.loadAsset(ENGINE_TEST2_URN, new AlternateAssetData());

        assertEquals(ImmutableSet.of(ENGINE_TEST_URN, ENGINE_TEST2_URN), assetManager.getLoadedAssetUrns(ParentAsset.class));
    }

    @Test
    public void getLoadedAssets() {
        Text asset = textAssetType.loadAsset(ENGINE_TEST_URN, new TextData("moo"));
        assertEquals(ImmutableSet.of(asset), assetManager.getLoadedAssets(Text.class));
    }

    @Test
    public void getLoadedAssetsCombinesInherited() {
        ChildAsset asset1 = childAssetType.loadAsset(ENGINE_TEST_URN, new ChildAssetData());
        AlternateAsset asset2 = alternateAssetType.loadAsset(ENGINE_TEST2_URN, new AlternateAssetData());
        Set<ParentAsset> expected = ImmutableSet.of(asset1, asset2);
        Set<ParentAsset> actual = assetManager.getLoadedAssets(ParentAsset.class);
        assertEquals(expected, actual);
    }

    @Test
    public void resolveWithoutContext() {
        AssetDataProducer<ChildAssetData> childProducer = mock(AssetDataProducer.class);
        when(childProducer.getModulesProviding(ENGINE_TEST_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST_URN.getModuleName()));
        childAssetType.addProducer(childProducer);

        AssetDataProducer<AlternateAssetData> alternateProducer = mock(AssetDataProducer.class);
        when(alternateProducer.getModulesProviding(ENGINE_TEST2_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST2_URN.getModuleName()));
        alternateAssetType.addProducer(alternateProducer);

        assertEquals(ImmutableSet.of(ENGINE_TEST_URN), assetManager.resolve(ENGINE_TEST_URN.getResourceName().toString(), ParentAsset.class));
        assertEquals(ImmutableSet.of(ENGINE_TEST2_URN), assetManager.resolve(ENGINE_TEST2_URN.getResourceName().toString(), ParentAsset.class));
    }

    @Test
    public void resolveWithExplicitContext() {
        AssetDataProducer<ChildAssetData> childProducer = mock(AssetDataProducer.class);
        when(childProducer.getModulesProviding(ENGINE_TEST_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST_URN.getModuleName()));
        childAssetType.addProducer(childProducer);

        AssetDataProducer<AlternateAssetData> alternateProducer = mock(AssetDataProducer.class);
        when(alternateProducer.getModulesProviding(ENGINE_TEST2_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST2_URN.getModuleName()));
        alternateAssetType.addProducer(alternateProducer);

        assertEquals(ImmutableSet.of(ENGINE_TEST_URN), assetManager.resolve(ENGINE_TEST_URN.getResourceName().toString(), ParentAsset.class, ENGINE_TEST_URN.getModuleName()));
        assertEquals(ImmutableSet.of(ENGINE_TEST2_URN), assetManager.resolve(ENGINE_TEST2_URN.getResourceName().toString(),
                ParentAsset.class, ENGINE_TEST2_URN.getModuleName()));
    }

    @Test
    public void resolveWithContextFromContextManager() {
        AssetDataProducer<ChildAssetData> childProducer = mock(AssetDataProducer.class);
        when(childProducer.getModulesProviding(ENGINE_TEST_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST_URN.getModuleName()));
        childAssetType.addProducer(childProducer);

        AssetDataProducer<AlternateAssetData> alternateProducer = mock(AssetDataProducer.class);
        when(alternateProducer.getModulesProviding(ENGINE_TEST2_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST2_URN.getModuleName()));
        alternateAssetType.addProducer(alternateProducer);

        try (Context ignored = ContextManager.beginContext(ENGINE_TEST_URN.getModuleName())) {
            assertEquals(ImmutableSet.of(ENGINE_TEST_URN), assetManager.resolve(ENGINE_TEST_URN.getResourceName().toString(), ParentAsset.class));
            assertEquals(ImmutableSet.of(ENGINE_TEST2_URN), assetManager.resolve(ENGINE_TEST2_URN.getResourceName().toString(), ParentAsset.class));
        }
    }

    @Test
    public void resolveCombinesAcrossInherited() {
        AssetDataProducer<ChildAssetData> childProducer = mock(AssetDataProducer.class);
        when(childProducer.getModulesProviding(ENGINE_TEST_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST_URN.getModuleName()));
        childAssetType.addProducer(childProducer);

        AssetDataProducer<AlternateAssetData> alternateProducer = mock(AssetDataProducer.class);
        when(alternateProducer.getModulesProviding(MORE_TEST_URN.getResourceName())).thenReturn(ImmutableSet.of(MORE_TEST_URN.getModuleName()));
        alternateAssetType.addProducer(alternateProducer);

        assertEquals(ImmutableSet.of(ENGINE_TEST_URN, MORE_TEST_URN), assetManager.resolve(ENGINE_TEST_URN.getResourceName().toString(), ParentAsset.class));
    }

    @Test
    public void getAsset() {
        Text text = textAssetType.loadAsset(ENGINE_TEST_URN, new TextData("moo"));
        assertEquals(text, assetManager.getAsset(ENGINE_TEST_URN, Text.class).get());
    }

    @Test
    public void getAssetAcrossInherited() {
        ChildAsset child = childAssetType.loadAsset(ENGINE_TEST_URN, new ChildAssetData());
        AlternateAsset alternate = alternateAssetType.loadAsset(ENGINE_TEST2_URN, new AlternateAssetData());
        assertEquals(child, assetManager.getAsset(ENGINE_TEST_URN, ParentAsset.class).get());
        assertEquals(alternate, assetManager.getAsset(ENGINE_TEST2_URN, ParentAsset.class).get());
    }

    @Test
    public void getAssetAcrossInheritedFailsIfMultipleResolutionOptions() {

        AssetDataProducer<ChildAssetData> childProducer = mock(AssetDataProducer.class, new OptionalAnswer());
        when(childProducer.redirect(any(ResourceUrn.class))).then(Return.firstArgument());
        when(childProducer.getModulesProviding(ENGINE_TEST_URN.getResourceName())).thenReturn(ImmutableSet.of(ENGINE_TEST_URN.getModuleName()));
        childAssetType.addProducer(childProducer);

        AssetDataProducer<AlternateAssetData> alternateProducer = mock(AssetDataProducer.class, new OptionalAnswer());
        when(alternateProducer.redirect(any(ResourceUrn.class))).then(Return.firstArgument());
        when(alternateProducer.getModulesProviding(MORE_TEST_URN.getResourceName())).thenReturn(ImmutableSet.of(MORE_TEST_URN.getModuleName()));
        alternateAssetType.addProducer(alternateProducer);

        childAssetType.loadAsset(ENGINE_TEST_URN, new ChildAssetData());
        alternateAssetType.loadAsset(MORE_TEST_URN, new AlternateAssetData());
        assertFalse(assetManager.getAsset(ENGINE_TEST_URN.getResourceName().toString(), ParentAsset.class).isPresent());
    }

}
