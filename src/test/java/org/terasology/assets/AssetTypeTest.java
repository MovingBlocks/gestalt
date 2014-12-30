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
import org.terasology.assets.stubs.text.Text;
import org.terasology.assets.stubs.text.TextData;
import org.terasology.assets.stubs.text.TextFactory;
import org.terasology.assets.stubs.text.TextFormat;
import org.terasology.module.ClasspathModule;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModulePathScanner;
import org.terasology.module.ModuleRegistry;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.PermissionProvider;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.ResourceUrn;
import org.terasology.naming.Version;
import org.terasology.util.io.FileExtensionPathMatcher;

import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public static final String TEXT_VALUE = "Value";
    public static final String TEXT_VALUE_2 = "Value";

    public static final ResourceUrn URN = new ResourceUrn("test", "example");
    public static final String ASSET_FILE_NAME = "example.txt";
    public static final String TXT_EXTENSION = "txt";


    private ModuleRegistry moduleRegistry;
    private AssetType<Text, TextData> assetType = new AssetType<>(ASSET_TYPE_ID, FOLDER_NAME, Text.class);

    public AssetTypeTest() throws Exception {
        moduleRegistry = new TableModuleRegistry();
        ModuleMetadata testModuleMetadata = new ModuleMetadata();
        testModuleMetadata.setId(new Name("test"));
        testModuleMetadata.setVersion(new Version("1.0.0"));
        Module testModule = ClasspathModule.create(testModuleMetadata, true, getClass());
        moduleRegistry.add(testModule);

        ModulePathScanner scanner = new ModulePathScanner();
        for (Path path : testModule.getLocations()) {
            Path virtualPath;
            if (Files.isRegularFile(path)) {
                FileSystem jarFileSystem = FileSystems.newFileSystem(path, null);
                virtualPath = jarFileSystem.getPath("virtualModules");
            } else {
                virtualPath = path.resolve("virtualModules");
            }
            if (Files.isDirectory(virtualPath)) {
                scanner.scan(moduleRegistry, virtualPath);
            }
        }
    }

    @Test
    public void construction() {
        assertEquals(new Name(ASSET_TYPE_ID), assetType.getId());
        assertEquals(FOLDER_NAME, assetType.getFolderName());
        assertEquals(Text.class, assetType.getAssetClass());
    }

    @Test
    public void loadData() {
        AssetFactory<Text, TextData> factory = mock(AssetFactory.class);
        TextData data = new TextData(TEXT_VALUE);
        assetType.setFactory(factory);
        Text text = new Text(URN, data);
        when(factory.build(URN, data)).thenReturn(text);

        Text createdText = assetType.loadAsset(URN, data);
        assertEquals(text, createdText);
        verify(factory).build(URN, data);
    }

    @Test
    public void retrieveLoadedDataByUrn() {
        assetType.setFactory(new TextFactory());
        TextData data = new TextData(TEXT_VALUE);

        Text loadedText = assetType.loadAsset(URN, data);
        Text retrievedText = assetType.getAsset(URN);
        assertEquals(loadedText, retrievedText);
    }

    @Test
    public void loadingAssetWithSameUrnReloadsExistingAsset() {
        assetType.setFactory(new TextFactory());
        TextData initialData = new TextData(TEXT_VALUE);
        Text initialText = assetType.loadAsset(URN, initialData);
        TextData newData = new TextData(TEXT_VALUE_2);

        Text newText = assetType.loadAsset(URN, newData);
        assertSame(initialText, newText);
        assertEquals(newData.getValue(), initialText.getValue());
    }

    @Test
    public void changingFactoryDisposesAllAssets() {
        assetType.setFactory(new TextFactory());
        TextData data = new TextData(TEXT_VALUE);
        Text asset = assetType.loadAsset(URN, data);

        assetType.setFactory(mock(AssetFactory.class));
        assertTrue(asset.isDisposed());
        assertNull(assetType.getAsset(URN));
    }

    @Test
    public void disposingAsset() {
        assetType.setFactory(new TextFactory());
        TextData data = new TextData(TEXT_VALUE);
        Text asset = assetType.loadAsset(URN, data);

        assetType.dispose(URN);
        assertTrue(asset.isDisposed());
        assertNull(assetType.getAsset(URN));
    }

    @Test
    public void resolveAssetWithUrn() throws Exception {
        AssetFactory<Text, TextData> factory = mock(AssetFactory.class);
        TextData data = new TextData(TEXT_VALUE);
        assetType.setFactory(factory);
        Text text = new Text(URN, data);
        when(factory.build(URN, data)).thenReturn(text);

        AssetFormat<TextData> format = mock(AssetFormat.class);
        when(format.getAssetName(ASSET_FILE_NAME)).thenReturn(URN.getResourceName());
        when(format.getFileMatcher()).thenReturn(new FileExtensionPathMatcher(TXT_EXTENSION));
        when(format.load(eq(URN), any(List.class))).thenReturn(data);
        assetType.addFormat(format);

        assetType.setEnvironment(createEnvironment());

        assertEquals(text, assetType.getAsset(URN));
    }

    @Test
    public void stringResolveFullUrn() throws Exception {
        assetType.setFactory(new TextFactory());
        assetType.addFormat(new TextFormat());
        assetType.setEnvironment(createEnvironment());

        Text asset = assetType.getAsset(URN.toString());
        assertNotNull(asset);
        assertEquals(URN, asset.getUrn());
    }

    @Test
    public void resolvePartialUrnWithSingleMatch() throws Exception {
        assetType.setFactory(new TextFactory());
        assetType.addFormat(new TextFormat());
        assetType.setEnvironment(createEnvironment());

        Text asset = assetType.getAsset(URN.getResourceName().toString());
        assertNotNull(asset);
        assertEquals(URN, asset.getUrn());
    }

    @Test
    public void resolvePartialUrnWithMultipleMatchesFails() throws Exception {
        assetType.setFactory(new TextFactory());
        assetType.addFormat(new TextFormat());
        assetType.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")), moduleRegistry.getLatestModuleVersion(new Name("moduleA"))));

        Text asset = assetType.getAsset("example");
        assertNull(asset);
    }

    @Test
    public void resolvePartialUrnInContext() throws Exception {
        assetType.setFactory(new TextFactory());
        assetType.addFormat(new TextFormat());
        assetType.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")), moduleRegistry.getLatestModuleVersion(new Name("moduleA"))));

        Text asset = assetType.getAsset("example", new Name("moduleA"));
        assertNotNull(asset);
        assertEquals(new ResourceUrn("moduleA:example"), asset.getUrn());
    }

    @Test
    public void resolvePartialUrnInContextDependency() throws Exception {
        assetType.setFactory(new TextFactory());
        assetType.addFormat(new TextFormat());
        assetType.setEnvironment(createEnvironment(moduleRegistry.getLatestModuleVersion(new Name("test")),
                moduleRegistry.getLatestModuleVersion(new Name("moduleA")),
                moduleRegistry.getLatestModuleVersion(new Name("moduleB"))));

        Text asset = assetType.getAsset("example", new Name("moduleB"));
        assertNotNull(asset);
        assertEquals(URN, asset.getUrn());
    }

    private ModuleEnvironment createEnvironment(Module... modules) throws URISyntaxException {
        return new ModuleEnvironment(Lists.newArrayList(modules), new PermissionProviderFactory() {
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
    }

    private ModuleEnvironment createEnvironment() throws URISyntaxException {
        return new ModuleEnvironment(Lists.<Module>newArrayList(moduleRegistry.getLatestModuleVersion(new Name("test"))), new PermissionProviderFactory() {
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
    }

}
