// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.beans;

import org.terasology.context.Lifetime;
import org.terasology.gestalt.di.ServiceRegistry;

public class TestRegistry extends ServiceRegistry {
    public TestRegistry(){
        this.with(Dep4.class).use(() -> new Dep4());
        this.with(Dep2.class).use(() -> new Dep2());
        this.with(Dep3.class)
            .lifetime(Lifetime.Singleton)
            .named("dep3");
        this.with(Dep1.class);
        this.with(ParentDep.class);
    }
}
