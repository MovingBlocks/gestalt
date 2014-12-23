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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.terasology.util.io.FileExtensionPathMatcher;
import org.terasology.assets.stubs.text.Text;
import org.terasology.assets.stubs.text.TextData;
import org.terasology.assets.stubs.text.TextFactory;
import org.terasology.module.ClasspathModule;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.PermissionProvider;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;

import java.security.Permission;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class AssetTypeTest {

    public static final String ASSET_TYPE_ID = "text";
    public static final String FOLDER_NAME = "text";

    public static final ResourceUrn URN = new ResourceUrn("engine", "testAssetPleaseIgnore");


    private AssetType<Text, TextData> assetType = new AssetType<>(ASSET_TYPE_ID, FOLDER_NAME, Text.class);

    @Test
    public void construction() {

        assertEquals(new Name(ASSET_TYPE_ID), assetType.getId());
        assertEquals(FOLDER_NAME, assetType.getFolderName());
        assertEquals(Text.class,  assetType.getAssetClass());
    }

    @Test
    public void loadData() {
        AssetFactory<Text, TextData> factory = mock(AssetFactory.class);
        TextData data = new TextData("Value");
        assetType.setFactory(factory);
        Text book = new Text(URN, data);
        when(factory.build(URN, data)).thenReturn(book);

        Text createdBook = assetType.loadAsset(URN, data);
        assertEquals(book, createdBook);
        verify(factory).build(URN, data);
    }

    @Test
    public void retrieveLoadedDataByUrn() {
        assetType.setFactory(new TextFactory());
        TextData data = new TextData("Body");

        Text loadedBook = assetType.loadAsset(URN, data);
        Text retrievedBook = assetType.getAsset(URN);
        assertEquals(loadedBook, retrievedBook);
    }

    @Test
    public void loadingAssetWithSameUrnReloadsExistingAsset() {
        assetType.setFactory(new TextFactory());
        TextData initialData = new TextData("Body");
        Text initialBook = assetType.loadAsset(URN, initialData);
        TextData newData = new TextData("Body2");

        Text newBook = assetType.loadAsset(URN, newData);
        assertSame(initialBook, newBook);
        assertEquals(newData.getValue(), initialBook.getValue());
    }

    @Test
    public void changingFactoryDisposesAllAssets() {
        assetType.setFactory(new TextFactory());
        TextData data = new TextData("Body");
        Text asset = assetType.loadAsset(URN, data);

        assetType.setFactory(mock(AssetFactory.class));
        assertTrue(asset.isDisposed());
        assertNull(assetType.getAsset(URN));
    }

    @Test
    public void disposingAsset() {
        assetType.setFactory(new TextFactory());
        TextData data = new TextData("Body");
        Text asset = assetType.loadAsset(URN, data);

        assetType.dispose(URN);
        assertTrue(asset.isDisposed());
        assertNull(assetType.getAsset(URN));
    }

    @Test
    public void resolveAssetWithUrn() throws Exception {
        AssetFactory<Text, TextData> factory = mock(AssetFactory.class);
        TextData data = new TextData("value");
        assetType.setFactory(factory);
        Text book = new Text(new ResourceUrn("test:example"), data);
        when(factory.build(new ResourceUrn("test:example"), data)).thenReturn(book);

        AssetFormat<TextData> format = mock(AssetFormat.class);
        when(format.getAssetName("example.txt")).thenReturn(new Name("example"));
        when(format.getFileMatcher()).thenReturn(new FileExtensionPathMatcher("txt"));
        when(format.load(eq(new ResourceUrn("test:example")), any(List.class))).thenReturn(data);
        assetType.addFormat(format);

        ModuleMetadata testModuleMetadata = new ModuleMetadata();
        testModuleMetadata.setId(new Name("test"));
        ClasspathModule module = ClasspathModule.create(testModuleMetadata, true, getClass());
        ModuleEnvironment environment = new ModuleEnvironment(Lists.<Module>newArrayList(module), new PermissionProviderFactory() {
            @Override
            public PermissionProvider createPermissionProviderFor(Module module) {
                return new PermissionProvider() {
                    @Override
                    public boolean isPermitted(Class aClass) {
                        return true;
                    }

                    @Override
                    public boolean isPermitted(Permission permission, Class<?> aClass) {
                        return false;
                    }
                };
            }
        }, Collections.<BytecodeInjector>emptyList());
        assetType.setEnvironment(environment);

        assertEquals(book, assetType.getAsset(new ResourceUrn("test:example")));
    }

}
