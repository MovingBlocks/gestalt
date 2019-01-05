package org.terasology.module.resources;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A ModuleFileSource that reads files from a directory on the file system, using nio.
 */
@RequiresApi(26)
public class PathFileSource implements ModuleFileSource {

    private static final Logger logger = LoggerFactory.getLogger(PathFileSource.class);

    private final Path rootPath;
    private final Predicate<Path> filter;

    /**
     * Creates a standard PathFileSource, excluding .class files from the given path
     * @param rootPath The path to expose files from
     */
    public PathFileSource(Path rootPath) {
        this(rootPath, x -> !x.getFileName().toString().endsWith(".class"));
    }

    /**
     * @param rootPath The path to expose files from
     * @param fileFilter A filter on the files that will be exposed
     */
    public PathFileSource(Path rootPath, Predicate<Path> fileFilter) {
        Preconditions.checkArgument(Files.isDirectory(rootPath), "Not a directory");
        this.rootPath = rootPath.normalize();
        this.filter = fileFilter;
    }

    @Override
    public Optional<ModuleFile> getFile(List<String> filepath) {
        Path fullPath = buildPath(filepath);
        if (fullPath.startsWith(rootPath) && Files.isRegularFile(fullPath) && filter.test(fullPath)) {
            return Optional.of(new PathFile(rootPath, rootPath.relativize(fullPath)));
        }
        return Optional.empty();
    }

    @Override
    public Collection<ModuleFile> getFilesInPath(boolean recursive, List<String> partialPath) {
        Path fullPath = buildPath(partialPath);

        if (fullPath.startsWith(rootPath) && Files.isDirectory(fullPath)) {
            List<ModuleFile> result = Lists.newArrayList();
            try {
                Files.walkFileTree(fullPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path x, BasicFileAttributes basicFileAttributes) {
                        if (filter.test(x)) {
                            result.add(new PathFile(rootPath, rootPath.relativize(x)));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                        if (fullPath.equals(path) || recursive) {
                            return FileVisitResult.CONTINUE;
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to scan subpath ({}) contents", partialPath, e);
            }
            return result;
        }
        return Collections.emptySet();
    }

    private Path buildPath(List<String> filepath) {
        Path fullPath = rootPath;
        for (String part : filepath) {
            fullPath = fullPath.resolve(part);
        }
        fullPath = fullPath.normalize();
        return fullPath;
    }

    @Override
    public Set<String> getSubpaths(List<String> fromPath) {
        Path fullPath = buildPath(fromPath);
        if (fullPath.startsWith(rootPath) && Files.isDirectory(fullPath)) {
            try (DirectoryStream<Path> subpaths = Files.newDirectoryStream(fullPath)) {
                return StreamSupport.stream(subpaths.spliterator(), false)
                        .filter(x -> Files.isDirectory(x))
                        .map(x -> x.getFileName().toString())
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                logger.error("Failed to read subpaths of " + fromPath, e);
            }
        }
        return Collections.emptySet();
    }

    @NonNull
    @Override
    public Iterator<ModuleFile> iterator() {
        return getFiles().iterator();
    }

    private class PathFile implements ModuleFile {

        private final Path rootPath;
        private final Path relativeFile;

        PathFile(Path rootPath, Path relativeFile) {
            this.rootPath = rootPath;
            this.relativeFile = relativeFile;
        }

        @Override
        public String getName() {
            return relativeFile.getFileName().toString();
        }

        @Override
        public List<String> getPath() {
            return StreamSupport.stream(relativeFile.getParent().spliterator(), false).map(Path::toString).collect(Collectors.toList());
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(rootPath.resolve(relativeFile));
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(rootPath, relativeFile);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof PathFile) {
                PathFile other = (PathFile) o;
                return Objects.equals(other.rootPath, this.rootPath) && Objects.equals(other.relativeFile, this.relativeFile);
            }
            return false;
        }
    }
}
