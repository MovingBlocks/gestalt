package org.terasology.gestalt.di.index;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created for using multiple {@link ClassIndex} at once.
 */
public class CompoundClassIndex implements ClassIndex {
    private final List<ClassIndex> indexes;

    public CompoundClassIndex(List<ClassIndex> indexes) {
        this.indexes = indexes;
    }

    public CompoundClassIndex() {
        this(Lists.newArrayList());
    }

    public void add(ClassIndex classIndex) {
        indexes.add(classIndex);
    }

    @Override
    public Set<String> getSubtypesOf(String clazzName) {
        return indexes.stream()
                .flatMap(classIndex -> classIndex.getSubtypesOf(clazzName).stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getTypesAnnotatedWith(String annotation) {
        return indexes.stream()
                .flatMap(classIndex -> classIndex.getTypesAnnotatedWith(annotation).stream())
                .collect(Collectors.toSet());
    }
}
