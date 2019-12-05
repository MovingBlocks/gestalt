package org.terasology.gestalt.assets;

/**
 * Interface for a resource that can be disposed. This is used by asset to register a resource
 * to be disposed of when an asset is disposed, or after it is garbage collected.
 */
public interface DisposableResource extends AutoCloseable {

    /**
     * Closes the asset. It is expected this should only happen once.
     */
    void close();
}
