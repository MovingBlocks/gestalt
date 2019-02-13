/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.util.reflection;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * SimpleClassFactory is a ClassFactory that will find a constructor that it is able to fill (from a parameter provider) and
 * constructor the requested class with it. It starts with the constructor with the most parameters and works down to the constructor
 * with the least parameters, using the first one it is able to satisfy. If multiple constructors have the same number of parameters
 * it is undefined which it will use.
 * <p>
 * SimpleClassFactory has special handling of {@link java.util.Optional} parameters - it will fill them with the appropriate object from the parameter provider
 * if possible, but otherwise will use {@link java.util.Optional#empty}. So it will be able to use constructors with Optional parameters regardless
 * of whether it has the desired object for them.
 * </p>
 *
 * @author Immortius
 */
public class SimpleClassFactory implements ClassFactory {
    private static final Logger logger = LoggerFactory.getLogger(SimpleClassFactory.class);

    private final ParameterProvider parameterProvider;

    /**
     * Creates a SimpleClassFactory that can only use default constructors
     */
    public SimpleClassFactory() {
        parameterProvider = new ParameterProvider() {
            @Override
            public <T> Optional<T> get(Class<T> x) {
                return Optional.empty();
            }
        };
    }

    /**
     * Creates a SimpleClassFactory that will use any constructor which requires the objects the given parameterProvider can provide.
     *
     * @param parameterProvider A provider of objects to use as constructor parameters.
     */
    public SimpleClassFactory(ParameterProvider parameterProvider) {
        this.parameterProvider = parameterProvider;
    }

    @Override
    public <T> Optional<T> instantiateClass(Class<? extends T> discoveredType) {
        List<Constructor<?>> possibleConstructors = Lists.newArrayList(discoveredType.getConstructors());
        possibleConstructors.sort(Comparator.<Constructor<?>, Integer>comparing(x -> x.getParameterTypes().length).reversed());

        for (Constructor<?> constructor : possibleConstructors) {
            boolean populatedAllParams = true;
            Object[] param = new Object[constructor.getParameterTypes().length];
            for (int i = 0; i < constructor.getParameterTypes().length; i++) {
                if (constructor.getParameterTypes()[i].equals(Optional.class)) {
                    Optional<Type> optionalType = GenericsUtil.getTypeParameterBinding(constructor.getGenericParameterTypes()[i], 0);
                    if (!optionalType.isPresent()) {
                        populatedAllParams = false;
                        break;
                    }
                    Class<?> paramClass = GenericsUtil.getClassOfType(optionalType.get());
                    param[i] = parameterProvider.get(paramClass);
                } else {
                    Optional<?> value = parameterProvider.get(constructor.getParameterTypes()[i]);
                    if (value.isPresent()) {
                        param[i] = value.get();
                    } else {
                        populatedAllParams = false;
                        break;
                    }
                }
            }
            if (populatedAllParams) {
                try {
                    return Optional.of(discoveredType.cast(constructor.newInstance(param)));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    logger.error("Failed to instantiate class: {}", discoveredType, e);
                    return Optional.empty();
                }
            }
        }

        logger.error("Type '{}' missing usable constructor", discoveredType);
        return Optional.empty();
    }

}
