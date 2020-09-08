package org.terasology.gestalt.support;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Parser {

    public static Iterable<? extends JavaFileObject> generate(JavaFileObject... sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        final StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnosticCollector, Locale.getDefault(), UTF_8);
        return null;
    }
}
