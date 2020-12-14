package org.terasology.context.exception;

public class DependencyInjectionException extends RuntimeException{

    public DependencyInjectionException() {
        super();
    }

    public DependencyInjectionException(String message) {
        super(message);
    }

    public DependencyInjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DependencyInjectionException(Throwable cause) {
        super(cause);
    }

    protected DependencyInjectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
