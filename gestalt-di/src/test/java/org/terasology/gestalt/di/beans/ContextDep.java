package org.terasology.gestalt.di.beans;

import org.terasology.context.annotation.Introspected;
import org.terasology.gestalt.di.BeanContext;

import javax.inject.Inject;

@Introspected
public class ContextDep {
    @Inject
    public BeanContext context;
}
