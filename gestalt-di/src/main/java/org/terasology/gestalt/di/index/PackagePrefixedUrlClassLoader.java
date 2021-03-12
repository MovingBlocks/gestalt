package org.terasology.gestalt.di.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class PackagePrefixedUrlClassLoader extends UrlClassIndex {

    private final String packagePrefix;

    protected PackagePrefixedUrlClassLoader(URL url, String packagePrefix) {
        super(url);
        this.packagePrefix = packagePrefix;
    }

    @Override
    protected Set<String> loadContent(String type, String target) {
        try {
            URL fullUrl = new URL(String.format("%s/%s/%s", getUrl(), type, target));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fullUrl.openStream()))) {
                return reader.lines()
                        .filter(line -> line.startsWith(packagePrefix))
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                return Collections.emptySet();
            }
        } catch (MalformedURLException e) {
            return Collections.emptySet();
        }
    }
}
