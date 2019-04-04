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

package org.terasology.gestalt.android.testbed;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.common.collect.Queues;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetType;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.entitysystem.component.ReflectionComponentTypeFactory;
import org.terasology.entitysystem.core.EntityManager;
import org.terasology.entitysystem.core.EntityRef;
import org.terasology.entitysystem.entity.inmemory.InMemoryEntityManager;
import org.terasology.entitysystem.transaction.TransactionManager;
import org.terasology.gestalt.android.AndroidModuleClassLoader;
import org.terasology.gestalt.android.testbed.assettypes.Text;
import org.terasology.gestalt.android.testbed.assettypes.TextData;
import org.terasology.gestalt.android.testbed.assettypes.TextFactory;
import org.terasology.gestalt.android.testbed.assettypes.TextFileFormat;
import org.terasology.gestalt.android.testbed.packageModuleA.TextComponent;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModulePathScanner;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.resources.FileReference;
import org.terasology.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.module.sandbox.WarnOnlyProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.test.api.ApiInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Deque;
import java.util.stream.Collectors;

public class GestaltTestActivity extends AppCompatActivity {

    private static Logger logger = LoggerFactory.getLogger(GestaltTestActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StringBuilder displayText = new StringBuilder();
        setContentView(R.layout.activity_gestalt_test);
        TextView text = findViewById(R.id.textDisplay);

        TableModuleRegistry moduleRegistry = new TableModuleRegistry();


        ModuleFactory factory = new ModuleFactory();
        factory.setScanningForClasses(false);

        ModuleMetadata metadataA = new ModuleMetadata();
        metadataA.setId(new Name("PackageModuleA"));
        metadataA.setVersion(new Version(1, 0, 0));
        moduleRegistry.add(factory.createPackageModule(metadataA, "org.terasology.gestalt.android.testbed.packageModuleA"));

        ModuleMetadata metadataB = new ModuleMetadata();
        metadataB.setId(new Name("PackageModuleB"));
        metadataB.setVersion(new Version(1, 0, 0));
        moduleRegistry.add(factory.createPackageModule(metadataB, "org.terasology.gestalt.android.testbed.packageModuleB"));

        copyModulesToData();

        ModulePathScanner pathScanner = new ModulePathScanner(factory);
        pathScanner.scan(moduleRegistry, getFilesDir());

        StandardPermissionProviderFactory permissionProviderFactory = new StandardPermissionProviderFactory();
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("java.lang");
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("org.terasology.test.api");
        ModuleEnvironment environment = new ModuleEnvironment(moduleRegistry, new WarnOnlyProviderFactory(permissionProviderFactory), (module, parent, permissionProvider) -> AndroidModuleClassLoader.create(module, parent, permissionProvider, getCodeCacheDir()));


        displayText.append("-== Module Content ==-\n\n");
        for (Module module : environment.getModulesOrderedByDependencies()) {
            displayText.append("==" + module.getId() + "==\n");
            for (FileReference fileReference : module.getResources().getFiles()) {
                displayText.append("+ " + fileReference.getName() + "\n");
                if (fileReference.getName().endsWith(".asset")) {
                    try (Reader reader = new InputStreamReader(fileReference.open())) {
                        displayText.append("  '");
                        displayText.append(CharStreams.toString(reader));
                        displayText.append("'\n");
                    } catch (IOException e) {
                        displayText.append("Error: ");
                        displayText.append(e.getMessage());
                        displayText.append("\n");
                    }
                }
            }
            displayText.append("\n");
        }

        displayText.append("-== Module Classes ==-\n");
        for (Class<? extends ApiInterface> textProducerClass : environment.getSubtypesOf(ApiInterface.class)) {
            try {
                ApiInterface textProducer = textProducerClass.newInstance();
                displayText.append(textProducer.getClass().getSimpleName())
                        .append(": \"")
                        .append(textProducer.apiMethod())
                        .append("\"\n");
            } catch (IllegalAccessException | InstantiationException e) {
                displayText.append("Error: ");
                displayText.append(e.getMessage());
                displayText.append("\n");
            }
        }

        displayText.append("\n-== Module Assets ==-\n");
        ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManagerImpl();

        AssetType<Text, TextData> assetType = assetTypeManager.createAssetType(Text.class, new TextFactory(), "text");
        assetTypeManager.getAssetFileDataProducer(assetType).addAssetFormat(new TextFileFormat());
        assetTypeManager.switchEnvironment(environment);


        for (ResourceUrn assetUrn : assetType.getAvailableAssetUrns()) {
            assetType.getAsset(assetUrn).ifPresent(x -> {
                displayText.append(assetUrn.toString())
                        .append(" - \"")
                        .append(x.getValue())
                        .append("\"\n");
            });
        }
        assetTypeManager.disposedUnusedAssets();

        TransactionManager transactionManager = new TransactionManager();
        EntityManager entityManager = new InMemoryEntityManager(new ReflectionComponentTypeFactory(), transactionManager);
        transactionManager.begin();
        EntityRef entity = entityManager.createEntity();
        TextComponent textComponent = entity.addComponent(TextComponent.class);
        textComponent.setText("Hello");
        transactionManager.commit();
        transactionManager.begin();
        displayText.append("\n-== Entity System Test ==-\n");
        displayText.append(entity.getComponent(TextComponent.class).get().getText());
        transactionManager.rollback();

        text.setText(displayText);
    }

    private void copyModulesToData() {

        Deque<String> toCopy = Queues.newArrayDeque();
        try {
            toCopy.addAll(Arrays.asList(getAssets().list("modules")));
            while (!toCopy.isEmpty()) {
                String assetPath = toCopy.pop();
                String[] contents = getAssets().list("modules/" + assetPath);
                if (contents.length > 0) {
                    File dir = new File(getFilesDir(), assetPath);
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    toCopy.addAll(Arrays.stream(contents).map(x -> assetPath + "/" + x).collect(Collectors.toList()));
                } else {
                    File file = new File(getFilesDir(), assetPath);
                    if (file.exists()) {
                        file.delete();
                    }
                    try (FileOutputStream out = new FileOutputStream(file); InputStream in = getAssets().open("modules/" + assetPath)) {
                        ByteStreams.copy(in, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
