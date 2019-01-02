package org.terasology.module.resources;

import org.terasology.util.Varargs;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModuleFileSource extends Iterable<ModuleFile> {

    default Optional<ModuleFile> getFile(String path, String ... morePath) {
        return getFile(Varargs.combineToList(path, morePath));
    }

    Optional<ModuleFile> getFile(List<String> filepath);

    Collection<ModuleFile> getFiles();

    default Collection<ModuleFile> getFilesInPath(String ... path) {
        return getFilesInPath(Arrays.asList(path));
    }

    Collection<ModuleFile> getFilesInPath(List<String> path);

}
