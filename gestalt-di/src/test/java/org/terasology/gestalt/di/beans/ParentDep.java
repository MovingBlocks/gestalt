package org.terasology.gestalt.di.beans;

import org.terasology.context.annotation.Introspected;

import javax.inject.Inject;
import javax.inject.Singleton;

@Introspected
public class ParentDep {
    @Inject
    Dep3 dep;

    @Inject
    ParentDep(Dep1 one, Dep2 two) {

    }

    @Inject
    public void setDep4(Dep4 d){

    }

}

