// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.instance;

import org.terasology.gestalt.di.BeanContext;
import org.terasology.gestalt.di.BeanEnvironment;
import org.terasology.gestalt.di.BeanKey;
import org.terasology.gestalt.di.Lifetime;

import java.util.Optional;

public abstract class BeanProvider<T> implements AutoCloseable {
    protected final Lifetime lifetime;
    protected final BeanEnvironment environment;

    public BeanProvider(BeanEnvironment environment, Lifetime lifetime) {
        this.lifetime = lifetime;
        this.environment = environment;
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public abstract Optional<T> get(BeanKey identifier, BeanContext current, BeanContext scopedTo);

}
