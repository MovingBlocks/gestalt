// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context.utils;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.AnnotationMetadata;
import org.terasology.context.AnnotationValue;
import org.terasology.context.Lifetime;
import org.terasology.context.annotation.Scoped;
import org.terasology.context.annotation.Transient;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

/**
 * A set of utilities that helps with examining {@link AnnotationMetadata}
 */
public final class BeanUtilities {
    private static final Logger logger = LoggerFactory.getLogger(BeanUtilities.class);

    public static final Map<String, Class> COMMON_CLASS_MAP = new HashMap<>(34);
    public static final Map<String, Class> BASIC_TYPE_MAP = new HashMap<>(18);

    private static final Map<String, Class> PRIMITIVE_TYPE_MAP = new ImmutableMap.Builder<String, Class>()
            .put("int", Integer.TYPE)
            .put("boolean", Boolean.TYPE)
            .put("long", Long.TYPE)
            .put("byte", Byte.TYPE)
            .put("double", Double.TYPE)
            .put("float", Float.TYPE)
            .put("char", Character.TYPE)
            .put("short", Short.TYPE)
            .put("void", void.class).build();

    private static final Map<String, Class> PRIMITIVE_ARRAY_MAP = new ImmutableMap.Builder<String, Class>()
            .put("int", int[].class)
            .put("boolean", boolean[].class)
            .put("long", long[].class)
            .put("byte", byte[].class)
            .put("double", double[].class)
            .put("float", float[].class)
            .put("char", char[].class)
            .put("short", short[].class)
            .build();

    private BeanUtilities() {

    }


    static {
        COMMON_CLASS_MAP.put(boolean.class.getName(), boolean.class);
        COMMON_CLASS_MAP.put(byte.class.getName(), byte.class);
        COMMON_CLASS_MAP.put(int.class.getName(), int.class);
        COMMON_CLASS_MAP.put(long.class.getName(), long.class);
        COMMON_CLASS_MAP.put(double.class.getName(), double.class);
        COMMON_CLASS_MAP.put(float.class.getName(), float.class);
        COMMON_CLASS_MAP.put(char.class.getName(), char.class);
        COMMON_CLASS_MAP.put(short.class.getName(), short.class);

        COMMON_CLASS_MAP.put(boolean[].class.getName(), boolean[].class);
        COMMON_CLASS_MAP.put(byte[].class.getName(), byte[].class);
        COMMON_CLASS_MAP.put(int[].class.getName(), int[].class);
        COMMON_CLASS_MAP.put(long[].class.getName(), long[].class);
        COMMON_CLASS_MAP.put(double[].class.getName(), double[].class);
        COMMON_CLASS_MAP.put(float[].class.getName(), float[].class);
        COMMON_CLASS_MAP.put(char[].class.getName(), char[].class);
        COMMON_CLASS_MAP.put(short[].class.getName(), short[].class);

        COMMON_CLASS_MAP.put(Boolean.class.getName(), Boolean.class);
        COMMON_CLASS_MAP.put(Byte.class.getName(), Byte.class);
        COMMON_CLASS_MAP.put(Integer.class.getName(), Integer.class);
        COMMON_CLASS_MAP.put(Long.class.getName(), Long.class);
        COMMON_CLASS_MAP.put(Short.class.getName(), Short.class);
        COMMON_CLASS_MAP.put(Double.class.getName(), Double.class);
        COMMON_CLASS_MAP.put(Float.class.getName(), Float.class);
        COMMON_CLASS_MAP.put(Character.class.getName(), Character.class);
        COMMON_CLASS_MAP.put(String.class.getName(), String.class);
        COMMON_CLASS_MAP.put(CharSequence.class.getName(), CharSequence.class);

        BASIC_TYPE_MAP.put(UUID.class.getName(), UUID.class);
        BASIC_TYPE_MAP.put(BigDecimal.class.getName(), BigDecimal.class);
        BASIC_TYPE_MAP.put(BigInteger.class.getName(), BigInteger.class);
        BASIC_TYPE_MAP.put(URL.class.getName(), URL.class);
        BASIC_TYPE_MAP.put(URI.class.getName(), URI.class);
        BASIC_TYPE_MAP.put(TimeZone.class.getName(), TimeZone.class);
        BASIC_TYPE_MAP.put(Charset.class.getName(), Charset.class);
        BASIC_TYPE_MAP.put(Locale.class.getName(), Locale.class);
        BASIC_TYPE_MAP.put(Duration.class.getName(), Duration.class);
        BASIC_TYPE_MAP.put(Date.class.getName(), Date.class);
        BASIC_TYPE_MAP.put(LocalDate.class.getName(), LocalDate.class);
        BASIC_TYPE_MAP.put(Instant.class.getName(), Instant.class);
        BASIC_TYPE_MAP.put(ZonedDateTime.class.getName(), ZonedDateTime.class);
    }

    public static Lifetime resolveLifetime(AnnotationMetadata metadata) {
        if (metadata.hasAnnotation(Singleton.class)) {
            return Lifetime.Singleton;
        } else if (metadata.hasAnnotation(Scoped.class)) {
            return Lifetime.Scoped;
        } else if (metadata.hasAnnotation(Transient.class)) {
            return Lifetime.Transient;
        }

        if (metadata.hasStereotype(javax.inject.Qualifier.class)) {
            AnnotationValue<Annotation> ann = metadata.getAnnotationsByStereotype(javax.inject.Qualifier.class).stream().findFirst().get();
            if (ann.hasAnnotation(Singleton.class)) {
                return Lifetime.Singleton;
            } else if (ann.hasAnnotation(Scoped.class)) {
                return Lifetime.Scoped;
            } else if (ann.hasAnnotation(Transient.class)) {
                return Lifetime.Transient;
            }
        }
        return Lifetime.Transient;
    }

    /**
     * Attempt to load a class for the given name from the given class loader. This method should be used
     * as a last resort, and note that any usage of this method will create complications on GraalVM.
     *
     * @param name        The name of the class
     * @param classLoader The classloader. If null will fallback to attempt the thread context loader, otherwise the system loader
     * @return An optional of the class
     */
    public static Optional<Class> forName(String name, ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }

            Optional<Class> commonType = Optional.ofNullable(COMMON_CLASS_MAP.get(name));
            if (commonType.isPresent()) {
                return commonType;
            } else {
                logger.debug("Attempting to dynamically load class {}", name);
                Class<?> type = Class.forName(name, true, classLoader);
                logger.debug("Successfully loaded class {}", name);
                return Optional.of(type);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            logger.debug("Class {} is not present", name);

            return Optional.empty();
        }
    }
}
