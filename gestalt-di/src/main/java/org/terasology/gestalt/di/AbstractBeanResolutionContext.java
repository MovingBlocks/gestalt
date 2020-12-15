package org.terasology.gestalt.di;

import java.util.Optional;

public abstract class AbstractBeanResolutionContext {

    public void resolve(BeanIdentifier identifier){

    }

    public abstract <T> Optional<T> tryResolveFromBeanContext(BeanIdentifier identifier);
}
