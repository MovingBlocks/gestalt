package org.terasology.gestalt.di.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Load index from {@link URL}.
 * <p>Can load index from:</p>
 * <p>1. directory</p>
 * <p>2. archive</p>
 * <p>3. ClassLoader</p>
 */
public class UrlClassIndex implements ClassIndex {
    private static final String METAINF = "META-INF";
    // HACK: Android always claims that the URL of the "META-INF" directory or any of its subdirectories doesn't exist.
    //       It will return the URL of any files within the "META-INF" directory and its subdirectories though.
    private static final String METAINF_TEST_FILE = METAINF + "/gestalt-indexes-present";
    private static final String SUBTYPES = "subtypes";
    private static final String ANNOTATIONS = "annotations";
    private final URL url;

    protected UrlClassIndex(URL url) {
        this.url = url;
    }

    public static ClassIndex byDirectory(File file) {
        try {
            return new UrlClassIndex(new URL(file.toURI().toURL(), METAINF));
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
        return createClassIndex(classLoader, UrlClassIndex::new);
    }

    public static ClassIndex byClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return createClassIndex(contextClassLoader, UrlClassIndex::new);
    }

    public static ClassIndex byClassLoaderPrefix(String packagePrefix) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return createClassIndex(contextClassLoader, url -> new PackagePrefixedUrlClassLoader(url, packagePrefix));
    }

    /**
     * Returns the url of the parent directory of the url provided. This assumes that the URL uses a path
     * separator of "/", which is probable for those returned by a classloader.
     * @param url the url to process
     * @return The url of the parent directory of the url provided, or null if invalid.
     */
    private static URL getParentURL(URL url) {
        if (url == null) {
            return null;
        }

        try {
            String urlString = url.toString();
            return new URL(urlString.substring(0, urlString.lastIndexOf('/')));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static ClassIndex createClassIndex(ClassLoader classLoader, Function<URL, ClassIndex> classIndexCreator) {
        try {
            CompoundClassIndex classIndex = new CompoundClassIndex();
            Enumeration<URL> resources = classLoader.getResources(METAINF_TEST_FILE);
            while (resources.hasMoreElements()) {
                URL resource = getParentURL(resources.nextElement());
                classIndex.add(classIndexCreator.apply(resource));
            }
            return classIndex;
        } catch (IOException e) {
            URL resource = getParentURL(classLoader.getResource(METAINF_TEST_FILE));
            return classIndexCreator.apply(resource);
        }
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
