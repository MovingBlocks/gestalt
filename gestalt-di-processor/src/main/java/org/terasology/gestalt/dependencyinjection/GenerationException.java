package org.terasology.gestalt.dependencyinjection;

/**
 * Exception if something goes wrong during generation
 */
class GenerationException extends Exception {
    public GenerationException() {
    }

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
