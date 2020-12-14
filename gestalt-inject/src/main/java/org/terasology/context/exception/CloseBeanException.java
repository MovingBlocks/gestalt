package org.terasology.context.exception;

public class CloseBeanException extends DependencyInjectionException{
    public CloseBeanException() {
        super();
    }

    public CloseBeanException(String message) {
        super(message);
    }

    public CloseBeanException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloseBeanException(Throwable cause) {
        super(cause);
    }

    protected CloseBeanException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
