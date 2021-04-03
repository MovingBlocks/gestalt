// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

public class SoftServiceLoader<S> implements Iterable<S> {
    public static final String META_INF_SERVICES = "META-INF/services";
    private static final Logger logger = LoggerFactory.getLogger(SoftServiceLoader.class);

    private final Class<S> target;
    private final ClassLoader classLoader;
    private final boolean loadsFromParent;

    public SoftServiceLoader(Class<S> target, ClassLoader classLoader) {
        this(target, classLoader, true);
    }

    public SoftServiceLoader(Class<S> target, ClassLoader classLoader, boolean loadsFromParent) {
        Preconditions.checkArgument(loadsFromParent || classLoader instanceof URLClassLoader || classLoader instanceof FindResourcesClassLoader,
                "loadsFromParent == false implemented for UrlClassLoader or FindResourcesClassLoader only");
        this.target = target;
        this.classLoader = classLoader;
        this.loadsFromParent = loadsFromParent;
    }

    public static <S> SoftServiceLoader<S> threadLocal(Class<S> target) {
        return new SoftServiceLoader<>(target, SoftServiceLoader.class.getClassLoader());
    }

    @Override
    @Nonnull
    public Iterator<S> iterator() {
        try {
            Enumeration<URL> urls;
            if (!loadsFromParent && classLoader instanceof URLClassLoader) {
                urls = ((URLClassLoader) classLoader).findResources(META_INF_SERVICES + '/' + target.getName());
            } else if (!loadsFromParent && classLoader instanceof FindResourcesClassLoader) {
                urls = ((FindResourcesClassLoader) classLoader).findResources(META_INF_SERVICES + '/' + target.getName());
            } else {
                urls = classLoader.getResources(META_INF_SERVICES + '/' + target.getName());
            }
            return Iterators.filter(new ServiceIterator(urls, target.getName()), Objects::nonNull);
        } catch (IOException e) {
            logger.error("Cannot iterate [{}]", META_INF_SERVICES + '/' + target.getName(), e);
        }
        return Collections.emptyIterator();
    }


    private class ServiceIterator implements Iterator<S> {
        private final Enumeration<URL> urls;
        private final String targetClass;
        private Iterator<String> nameIterator;

        public ServiceIterator(Enumeration<URL> urls, String targetClass) {
            this.urls = urls;
            nameIterator = Collections.emptyIterator();
            this.targetClass = targetClass;
        }

        @Override
        public boolean hasNext() {
            if (nameIterator.hasNext()) {
                return true;
            }
            if (urls.hasMoreElements()) {
                while (!nameIterator.hasNext()) {
                    URL url = urls.nextElement();
                    switchNameIteratorTo(url);
                }
                return true;
            }
            return false;
        }

        @Override
        public S next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String className = nameIterator.next();
            try {
                Class<?> clazz = classLoader.loadClass(className);
                if (clazz != null) {
                    return (S) clazz.getDeclaredConstructor().newInstance();
                } else {
                    logger.warn("Cannot receive {}'s service: [{}]", targetClass, className);
                    return null;
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                logger.warn("Cannot create {}'s service", targetClass, e);
            }
            return null;
        }

        private void switchNameIteratorTo(URL url) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                List<String> lines = reader.lines()
                        .filter(line -> line.length() != 0 && line.charAt(0) != '#')
                        .map(line -> {
                            int i = line.indexOf('#');
                            if (i > -1) {
                                return line.substring(0, i);
                            }
                            return line;
                        }).collect(Collectors.toList());

                nameIterator = lines.listIterator();
            } catch (IOException e) {
                logger.warn("Cannot read {}'s services", targetClass, e);
            }
        }
    }
}
