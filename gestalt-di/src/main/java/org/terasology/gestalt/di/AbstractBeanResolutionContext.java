package org.terasology.gestalt.di;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractBeanResolutionContext {

    private Queue<BeanIdentifier> processQueue = new ConcurrentLinkedQueue<BeanIdentifier>();

    public void resolve(BeanIdentifier identifier){

    }

    public abstract <T> Optional<T> tryResolveFromBeanContext(BeanIdentifier identifier);
}
