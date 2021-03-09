// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.beans;

import org.terasology.context.annotation.Introspected;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;
import javax.inject.Singleton;

@Introspected
public class ParentDep {
    @Inject
    @Named("dep3")
    Dep3 dep;

    @Inject
    ParentDep(Dep1 one, Dep2 two) {

    }

    @Inject
    public void setDep4(Dep4 d){

    }

    public Dep3 getDep() {
        return dep;
    }
}

