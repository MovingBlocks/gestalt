package org.terasology.gestalt.annotation.processing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedOptions("resource")
public class ResourceProcessor extends AbstractProcessor {
    private static final String FILE = "META-INF/resources";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            String paths = processingEnv.getOptions().get("resource");
            if (paths != null) {
                for (String path : paths.split(File.pathSeparator)) {
                    try {
                        Path root = Paths.get(path);
                        if (root.toFile().exists()) {
                            List<String> files = Files.walk(root)
                                    .map(root::relativize)
                                    .map(Objects::toString)
                                    .filter(str -> !str.endsWith(".class"))
                                    .filter(str -> !str.isEmpty())
                                    .collect(Collectors.toList());
                            FileObject fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", FILE);
                            try (BufferedWriter writer = new BufferedWriter(fileObject.openWriter())) {
                                for (String clazz : files) {
                                    writer.write(clazz);
                                    writer.newLine();
                                }
                            }
                        }
                    } catch (IOException e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Cannot locate resources [%s] with error [%s]", path, e.getMessage()));
                    }
                }
            }
        }
        return false;
    }
}
