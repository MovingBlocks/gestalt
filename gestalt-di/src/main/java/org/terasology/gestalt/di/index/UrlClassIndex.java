package org.terasology.gestalt.di.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class UrlClassIndex implements ClassIndex {
    private static final String METAINF = "META-INF";
    private static final String SUBTYPES = "subtypes";
    private static final String ANNOTATIONS = "annotations";
    private final URL url;

    protected UrlClassIndex(URL url) {
        this.url = url;
    }

    public static ClassIndex byDirectory(File file) {
        try {
            return new UrlClassIndex(new URL(file.toURI().toURL(), "/" + METAINF));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassIndex byArchive(File file) {
        try {
            return new UrlClassIndex(new URL(String.format("jar:file:///%s!/%s", file.getAbsolutePath(), METAINF)));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassIndex byClassLoader(ClassLoader classLoader) {
        return new UrlClassIndex(classLoader.getResource(METAINF));
    }

    public static ClassIndex byClassLoader() {
        return new UrlClassIndex(Thread.currentThread().getContextClassLoader().getResource(METAINF));
    }

    public static ClassIndex byClassLoaderPrefix(String packagePrefix) {
        return new PackagePrefixedUrlClassLoader(Thread.currentThread().getContextClassLoader().getResource(METAINF), packagePrefix);
    }

    protected URL getUrl() {
        return url;
    }

    @Override
    public Set<String> getSubtypesOf(String clazzName) {
        return loadContent(SUBTYPES, clazzName);
    }

    @Override
    public Set<String> getTypesAnnotatedWith(String annotation) {
        return loadContent(ANNOTATIONS, annotation);
    }

    protected Set<String> loadContent(String type, String target) {
        try {
            URL fullUrl = new URL(String.format("%s/%s/%s", url, type, target));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fullUrl.openStream()))) {
                return reader.lines().collect(Collectors.toSet());
            } catch (IOException e) {
                return Collections.emptySet();
            }
        } catch (MalformedURLException e) {
            return Collections.emptySet();
        }
    }
}
