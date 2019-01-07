package org.terasology.module.resources;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * EmptyFileSource, a null object for when no file source is desired.
 */
public class EmptyFileSource implements ModuleFileSource {

    @Override
    public Optional<ModuleFile> getFile(List<String> filepath) {
        return Optional.empty();
    }

    @Override
    public Collection<ModuleFile> getFilesInPath(boolean recursive, List<String> path) {
        return Collections.emptyList();
    }

    @Override
    public Set<String> getSubpaths(List<String> fromPath) {
        return Collections.emptySet();
    }

    @Override
    public Iterator<ModuleFile> iterator() {
        return Collections.emptyIterator();
    }
}
