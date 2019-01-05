package org.terasology.module.resources;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A ModuleFileSource that reads files from a directory on the file system, using Java's {@link File} class.
 */
public class DirectoryFileSource implements ModuleFileSource {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryFileSource.class);

    private final File rootDirectory;
    private final Predicate<File> filter;

    /**
     * Creates a standard DirectoryFileSource, excluding all .class files from the given directory
     * @param directory The directory to read resources from
     */
    public DirectoryFileSource(File directory) {
        this(directory, x -> !x.getName().endsWith(".class"));
    }

    /**
     * Creates a DirectoryFileSource
     * @param directory The directory to read resources from
     * @param contentFilter A predicate to filter which files are exposed
     */
    public DirectoryFileSource(File directory, Predicate<File> contentFilter) {
        Preconditions.checkArgument(directory.isDirectory(), "Not a directory");
        this.filter = contentFilter;
        try {
            this.rootDirectory = directory.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to canonicalize file " + directory, e);
        }
    }

    @Override
    public Collection<ModuleFile> getFiles() {
        return Lists.newArrayList(new DirectoryIterator(rootDirectory, rootDirectory, filter, true));
    }

    @Override
    public Optional<ModuleFile> getFile(List<String> filepath) {
        File file = buildFilePath(filepath);
        try {
            if (!file.getCanonicalPath().startsWith(rootDirectory.getPath())) {
                return Optional.empty();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        if (file.isFile() && filter.test(file)) {
            return Optional.of(new DirectoryFile(file, rootDirectory));
        }
        return Optional.empty();
    }

    private File buildFilePath(List<String> filepath) {
        File file = rootDirectory;
        for (String part : filepath) {
            file = new File(file, part);
        }
        return file;
    }

    @Override
    public Collection<ModuleFile> getFilesInPath(boolean recursive, List<String> path) {
        File dir = buildFilePath(path);
        try {
            if (!dir.getCanonicalPath().startsWith(rootDirectory.getPath())) {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(new DirectoryIterator(rootDirectory, dir, filter, recursive));
    }

    @Override
    public Set<String> getSubpaths(List<String> fromPath) {
        File dir = buildFilePath(fromPath);
        try {
            if (dir.getCanonicalPath().startsWith(rootDirectory.getPath())) {
                File[] contents = dir.listFiles();
                if (contents != null) {
                    return Arrays.asList(contents).stream().filter(x -> x.isDirectory()).map(x -> x.getName()).collect(Collectors.toSet());
                }
            }
        } catch(IOException e) {
            logger.error("Failed to canonicalize path", e);
        }
        return Collections.emptySet();
    }

    @NonNull
    @Override
    public Iterator<ModuleFile> iterator() {
        return new DirectoryIterator(rootDirectory, rootDirectory, filter, true);
    }

    private static class DirectoryFile implements ModuleFile {

        private final File baseDirectory;
        private final File file;

        DirectoryFile(File file, File baseDirectory) {
            this.file = file;
            this.baseDirectory = baseDirectory;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public List<String> getPath() {
            try {
                String filePath = file.getParentFile().getCanonicalPath();
                String basePath = baseDirectory.getPath();

                return Arrays.asList(filePath.substring(basePath.length() + 1).split(Pattern.quote(File.separator)));
            } catch (IOException e) {
                logger.warn("Failed to canonicalize path", e);
                return Collections.emptyList();
            }
        }

        @Override
        public InputStream open() throws IOException {
            return new BufferedInputStream(new FileInputStream(file));
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof DirectoryFile) {
                DirectoryFile other = (DirectoryFile) o;
                return Objects.equals(other.file, this.file);
            }
            return false;
        }
    }

    private static class DirectoryIterator implements Iterator<ModuleFile> {

        private Deque<File> files = Queues.newArrayDeque();
        private ModuleFile next;
        private Predicate<File> filter;
        private boolean recursive;
        private File rootDirectory;

        DirectoryIterator(File rootDirectory, File baseDirectory, Predicate<File> filter, boolean recursive) {
            this.filter = filter;
            this.recursive = recursive;
            files.add(baseDirectory);
            addDirectoryContentsToQueue(baseDirectory);
            findNext();
        }

        private void findNext() {
            next = null;
            while (next == null && !files.isEmpty()) {
                File file = files.pop();
                if (file.isDirectory() && recursive) {
                    addDirectoryContentsToQueue(file);
                } else if (file.isFile() && filter.test(file)) {
                    next = new DirectoryFile(file, rootDirectory);
                }
            }
        }

        private void addDirectoryContentsToQueue(File file) {
            File[] contents = file.listFiles();
            if (contents != null) {
                files.addAll(Arrays.asList(contents));
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ModuleFile next() {
            ModuleFile result = next;
            findNext();
            return result;
        }
    }
}
