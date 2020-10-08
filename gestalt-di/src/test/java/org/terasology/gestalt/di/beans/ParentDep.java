package org.terasology.gestalt.di.beans;

import org.terasology.context.annotation.Introspected;

import javax.inject.Inject;
import javax.inject.Singleton;

@Introspected
public class ParentDep {
    @Inject
    ParentDep(Dep1 one, Dep2 two) {

    }
}
