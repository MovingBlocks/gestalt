/*
 * Copyright 2019 MovingBlocks
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

package org.terasology.assets.module.autoreload;

import com.google.common.collect.SetMultimap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.producer.FileChangeSubscriber;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.resources.FileReference;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.util.io.FilesUtil;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import virtualModules.test.stubs.text.Text;
import virtualModules.test.stubs.text.TextData;
import virtualModules.test.stubs.text.TextFactory;

import static org.junit.Assert.assertTrue;

public class ModuleEnvironmentWatcherTest {

    private static Logger logger = LoggerFactory.getLogger(ModuleEnvironmentWatcherTest.class);

    @Test
    public void test() throws IOException {
        Path tempDirectory = Files.createTempDirectory("gestalt-test");
        ModuleMetadata metadata = new ModuleMetadata(new Name("test"), Version.DEFAULT);
        Module module = new ModuleFactory().createDirectoryModule(metadata, tempDirectory.toFile());
        ModuleEnvironment environment = new ModuleEnvironment(Collections.singletonList(module), new PermitAllPermissionProviderFactory());
        ModuleEnvironmentWatcher watcher = new ModuleEnvironmentWatcher(environment);
        FileChangeSubscriber subscriber = new FileChangeSubscriber() {
            @Override
            public Optional<ResourceUrn> assetFileAdded(FileReference file, Name module, Name providingModule) {
                logger.info("Asset File added: {}:{} by {}", module, file, providingModule);
                return Optional.of(new ResourceUrn(module, new Name(file.getName())));
            }

            @Override
            public Optional<ResourceUrn> assetFileModified(FileReference file, Name module, Name providingModule) {
                logger.info("Asset File modified: {}:{} by {}", module, file, providingModule);
                return Optional.of(new ResourceUrn(module, new Name(file.getName())));
            }

            @Override
            public Optional<ResourceUrn> assetFileDeleted(FileReference file, Name module, Name providingModule) {
                logger.info("Asset File deleted: {}:{} by {}", module, file, providingModule);
                return Optional.empty();
            }

            @Override
            public Optional<ResourceUrn> deltaFileAdded(FileReference file, Name module, Name providingModule) {
                logger.info("Delta File added: {}:{} by {}", module, file, providingModule);
                return Optional.empty();
            }

            @Override
            public Optional<ResourceUrn> deltaFileModified(FileReference file, Name module, Name providingModule) {
                logger.info("Delta File modified: {}:{} by {}", module, file, providingModule);
                return Optional.empty();
            }

            @Override
            public Optional<ResourceUrn> deltaFileDeleted(FileReference file, Name module, Name providingModule) {
                logger.info("Delta File deleted: {}:{} by {}", module, file, providingModule);
                return Optional.empty();
            }
        };
        AssetType<Text, TextData> assetType = new AssetType<>(Text.class, new TextFactory());
        watcher.register("text", subscriber, assetType);
        watcher.checkForChanges();
        Files.createDirectories(tempDirectory.resolve("assets").resolve("text"));
        watcher.checkForChanges();
        Files.createFile(tempDirectory.resolve("assets").resolve("text").resolve("test.txt"));
        SetMultimap<AssetType<?, ?>, ResourceUrn> changed = watcher.checkForChanges();
        assertTrue(changed.containsEntry(assetType, new ResourceUrn(module.getId(), new Name("test.txt"))));
        try (Writer writer = Files.newBufferedWriter(tempDirectory.resolve("assets").resolve("text").resolve("test.txt"))) {
            writer.write("This is my text");
        }
        changed = watcher.checkForChanges();
        assertTrue(changed.containsEntry(assetType, new ResourceUrn(module.getId(), new Name("test.txt"))));
        try {
            FilesUtil.recursiveDelete(tempDirectory);
        } catch (IOException e) {
            // Observed on Windows 10 to fail - timing issue? Try again after a brief moment!
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {

            }
            FilesUtil.recursiveDelete(tempDirectory);
        }
        watcher.checkForChanges();

    }

}
