// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

import org.terasology.context.Argument;
import org.terasology.context.BeanResolution;
import org.terasology.context.SingleGenericArgument;
import org.terasology.context.utils.BeanUtilities;

import javax.inject.Provider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default Bean Resolution for resolving dependencies against {@link org.terasology.context.BeanDefinition}
 *
 * Depdencies are resolved against {@link BeanContext} and provided to {@link org.terasology.context.BeanDefinition} when building dependency
 */
public class DefaultBeanResolution implements BeanResolution {
    private final BeanContext beanContext;
    private final BeanEnvironment environment;

    public DefaultBeanResolution(BeanContext context, BeanEnvironment environment) {
        this.beanContext = context;
        this.environment = environment;
    }

    @Override
    public <T> Optional<T> resolveConstructorArgument(Class<T> target, Argument<T> argument) {
        return getBean(target, argument);
    }

    @Override
    public <T> Optional<T> resolveParameterArgument(Class<T> target, Argument<T> argument) {
        return getBean(target, argument);
    }

    private <T> Optional<T> getBean(Class<T> target, Argument<T> argument) {
        if (argument instanceof SingleGenericArgument) {
            BeanKey<T> key = BeanKey.resolveBeanKey(argument.getType(), argument)
                    .withAnnotations(argument.getAnnotation());
            if (target.isAssignableFrom(Provider.class)) {
                return (Optional<T>) Optional.of((Provider<T>) () -> beanContext.getBean(key));
            } else if (target.isAssignableFrom(List.class)) {
                return (Optional<T>) Optional.ofNullable(beanContext.getBeans(key));
            } else if (target.isAssignableFrom(Collection.class)) {
                return (Optional<T>) Optional.ofNullable(beanContext.getBeans(key));
            } else if (target.isAssignableFrom(Set.class)) {
                return (Optional<T>) Optional.ofNullable(beanContext.getBeans(key).stream().collect(Collectors.toSet()));
            }
            throw new UnsupportedOperationException("Cannot resolve field with type "+ target);
        } else {
            BeanKey<T> key = BeanKey.resolveBeanKey(target, argument)
                    .withAnnotations(argument.getAnnotation());
            return beanContext.findBean(key);
        }
    }
}
