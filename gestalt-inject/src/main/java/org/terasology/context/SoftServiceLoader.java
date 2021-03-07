// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class SoftServiceLoader<S> implements Iterable<S> {
    public static final String META_INF_SERVICES = "META-INF/services";

    private final Class<S> target;
    private final ClassLoader classLoader;

    public SoftServiceLoader(Class<S> target, ClassLoader classLoader) {
        this.target = target;
        this.classLoader = classLoader;
    }

    public static <S> SoftServiceLoader<S> ThreadLocal(Class<S> target) {
        return new SoftServiceLoader<>(target, SoftServiceLoader.class.getClassLoader());
    }

    @Override
    @Nonnull
    public Iterator<S> iterator() {
        try {
            Enumeration<URL> urls = classLoader.getResources(META_INF_SERVICES + '/' + target.getName());
            return new Iterator<S>() {
                private Iterator<String> nameIterator = Collections.emptyIterator();
                @Override
                public boolean hasNext() {
                    if(nameIterator.hasNext()) {
                        return true;
                    }
                    if(urls.hasMoreElements()) {
                        while (!nameIterator.hasNext()) {
                            URL url = urls.nextElement();
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

                            }
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
                    String clazz = nameIterator.next();
                    try {
                        return (S) classLoader.loadClass(clazz).getDeclaredConstructor().newInstance();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } catch (IOException e) {
//            throw e;
        }
        return Collections.emptyIterator();
    }


}
