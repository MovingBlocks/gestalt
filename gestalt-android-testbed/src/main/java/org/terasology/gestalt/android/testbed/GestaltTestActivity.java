package org.terasology.gestalt.android.testbed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.gestalt.android.testbed.engine.TextProducer;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.resources.ModuleFile;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class GestaltTestActivity extends AppCompatActivity {

    private static Logger logger = LoggerFactory.getLogger(GestaltTestActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StringBuilder displayText = new StringBuilder();
        setContentView(R.layout.activity_gestalt_test);
        TextView text = findViewById(R.id.textDisplay);

        List<Module> modules = Lists.newArrayList();
        ModuleFactory factory = new ModuleFactory();
        ModuleMetadata engineMetadata = new ModuleMetadata();
        engineMetadata.setId(new Name("Engine"));
        engineMetadata.setVersion(new Version(1, 0, 0));
        modules.add(factory.createPackageModule(engineMetadata, "org.terasology.gestalt.android.testbed.engine"));

        ModuleMetadata metadataA = new ModuleMetadata();
        metadataA.setId(new Name("ModuleA"));
        metadataA.setVersion(new Version(1, 0, 0));
        modules.add(factory.createPackageModule(metadataA, "org.terasology.gestalt.android.testbed.moduleA"));

        ModuleMetadata metadataB = new ModuleMetadata();
        metadataB.setId(new Name("ModuleB"));
        metadataB.setVersion(new Version(1, 0, 0));
        modules.add(factory.createPackageModule(metadataB, "org.terasology.gestalt.android.testbed.moduleB"));

        copyModulesToData();

        try {
            modules.add(factory.createDirectoryModule(new File(getFilesDir(), "directoryModule")));

        } catch (IOException e) {
            displayText.append("Error: ");
            displayText.append(e.getMessage());
            displayText.append("\n");
        }


        ModuleEnvironment environment = new ModuleEnvironment(modules, new PermitAllPermissionProviderFactory());

        for (Class<? extends TextProducer> textProducerClass : environment.getSubtypesOf(TextProducer.class)) {
            try {
                TextProducer textProducer = textProducerClass.newInstance();
                displayText.append(textProducer.getText());
                displayText.append("\n");
            } catch (IllegalAccessException | InstantiationException e) {
                displayText.append("Error: ");
                displayText.append(e.getMessage());
                displayText.append("\n");
            }
        }

        for (Module module : environment.getModulesOrderedByDependencies()) {
            displayText.append("==" + module.getId() + "==\n");
            for (ModuleFile moduleFile : module.getResources().getFiles()) {
                displayText.append("+ " + moduleFile.getName() + "\n");
                if (moduleFile.getName().endsWith(".asset")) {
                    try (Reader reader = new InputStreamReader(moduleFile.open())) {
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
                    toCopy.addAll(Arrays.asList(contents).stream().map(x -> assetPath + "/" + x).collect(Collectors.toList()));
                } else {
                    File file = new File(getFilesDir(), assetPath);
                    if (!file.exists()) {
                        try (FileOutputStream out = new FileOutputStream(file); InputStream in = getAssets().open("modules/" + assetPath)) {
                            ByteStreams.copy(in, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
