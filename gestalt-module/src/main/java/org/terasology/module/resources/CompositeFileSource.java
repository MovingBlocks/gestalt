package org.terasology.module.resources;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.terasology.util.Varargs;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CompositeFileSource combines multiple ModuleFileSources together to act as a single ModuleFileSource
 */
public class CompositeFileSource implements ModuleFileSource {

    private final List<ModuleFileSource> sources;

    /**
     * @param source The first (mandatory) file source
     * @param sources Any additional file sources
     */
    public CompositeFileSource(ModuleFileSource source, ModuleFileSource... sources) {
        Preconditions.checkNotNull(source);
        this.sources = Varargs.combineToList(source, sources);
    }

    /**
     * @param sources A non-empty list of file sources
     */
    public CompositeFileSource(List<ModuleFileSource> sources) {
        Preconditions.checkArgument(!sources.isEmpty());
        this.sources = ImmutableList.copyOf(sources);
    }

    @Override
    public Optional<ModuleFile> getFile(List<String> filepath) {
        for (ModuleFileSource source : sources) {
            Optional<ModuleFile> result = source.getFile(filepath);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<ModuleFile> getFiles() {
        return sources.stream().flatMap(x -> x.getFiles().stream()).collect(Collectors.toList());
    }

    @Override
    public Collection<ModuleFile> getFilesInPath(boolean recursive, List<String> path) {
        return sources.stream().flatMap(x -> x.getFilesInPath(recursive, path).stream()).collect(Collectors.toList());
    }

    @Override
    public Set<String> getSubpaths(List<String> fromPath) {
        return sources.stream().flatMap(x -> x.getSubpaths(fromPath).stream()).collect(Collectors.toSet());
    }

    @NonNull
    @Override
    public Iterator<ModuleFile> iterator() {
        return sources.stream().flatMap(x -> x.getFiles().stream()).iterator();
    }
}
