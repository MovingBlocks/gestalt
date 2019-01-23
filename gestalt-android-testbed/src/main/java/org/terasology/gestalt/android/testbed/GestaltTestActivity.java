package org.terasology.gestalt.android.testbed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

public class GestaltTestActivity extends AppCompatActivity {

    private static Logger logger = LoggerFactory.getLogger(GestaltTestActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestalt_test);
        TextView text = findViewById(R.id.textDisplay);

        ModuleFactory factory = new ModuleFactory();
        ModuleMetadata engineMetadata = new ModuleMetadata();
        engineMetadata.setId(new Name("Engine"));
        engineMetadata.setVersion(new Version(1, 0, 0));
        Module engine = factory.createPackageModule(engineMetadata, "org.terasology.gestalt.android.testbed.engine");

        ModuleMetadata metadataA = new ModuleMetadata();
        metadataA.setId(new Name("ModuleA"));
        metadataA.setVersion(new Version(1, 0, 0));
        Module moduleA = factory.createPackageModule(metadataA, "org.terasology.gestalt.android.testbed.moduleA");

        ModuleMetadata metadataB = new ModuleMetadata();
        metadataB.setId(new Name("ModuleB"));
        metadataB.setVersion(new Version(1, 0, 0));
        Module moduleB = factory.createPackageModule(metadataB, "org.terasology.gestalt.android.testbed.moduleB");

        ModuleEnvironment environment = new ModuleEnvironment(Arrays.asList(engine, moduleA, moduleB), new PermitAllPermissionProviderFactory());
        StringBuilder displayText = new StringBuilder();
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

//        InputStream stream = getClassLoader().getResourceAsStream("/org/terasology/gestalt/android/testbed/moduleA/testasset.asset");
//        logger.info("", stream);

        for (Module module : environment.getModulesOrderedByDependencies()) {
            for (ModuleFile moduleFile : module.getResources().getFiles()) {
                displayText.append(module.getId() + " contains resource " + moduleFile.getName());
                if (moduleFile.getName().endsWith(".asset")) {
                    try {
                        InputStream stream = moduleFile.open();
                    } catch (IOException e) {
                        logger.error("", e);
                    }
                    try (Reader reader = new InputStreamReader(moduleFile.open())) {
                        displayText.append(CharStreams.toString(reader));
                        displayText.append("\n");
                    } catch (IOException e) {
                        displayText.append("Error: ");
                        displayText.append(e.getMessage());
                        displayText.append("\n");
                    }
                }
                displayText.append("\n");
            }

        }


        text.setText(displayText);
    }

}
