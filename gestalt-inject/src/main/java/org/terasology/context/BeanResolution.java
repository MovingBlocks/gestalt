package org.terasology.context;

public interface BeanResolution {
    <T> T resolveConstructorArgument(Class<T> target, Argument<T> argument) throws Exception;
    <T> T resolveParameterArgument(Class<T> target, Argument<T> argument) throws Exception;
}
