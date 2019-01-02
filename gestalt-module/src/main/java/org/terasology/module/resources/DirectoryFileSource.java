package org.terasology.module.resources;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.util.Varargs;

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
import java.util.regex.Pattern;

public class DirectoryFileSource implements ModuleFileSource {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryFileSource.class);

    private final File directory;

    public DirectoryFileSource(File directory) {
        Preconditions.checkArgument(directory.isDirectory(), "Not a directory");
        this.directory = directory;
    }

    @Override
    public Collection<ModuleFile> getFiles() {
        return Lists.newArrayList(new DirectoryIterator(directory));
    }

    @Override
    public Optional<ModuleFile> getFile(List<String> filepath) {
        File file = directory;
        for (String part : filepath) {
            file = new File(file, part);
        }
        try {
            if (!file.getCanonicalPath().startsWith(directory.getCanonicalPath())) {
                return Optional.empty();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        if (file.exists() && file.isFile()) {
            return Optional.of(new DirectoryFile(file));
        }
        return Optional.empty();
    }

    @Override
    public Collection<ModuleFile> getFilesInPath(List<String> path) {
        File dir = directory;
        for (String part : path) {
            dir = new File(dir, part);
        }
        try {
            System.out.println(dir.getCanonicalPath());
            if (!dir.getCanonicalPath().startsWith(directory.getCanonicalPath())) {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(new DirectoryIterator(dir));
    }

    @NonNull
    @Override
    public Iterator<ModuleFile> iterator() {
        return new DirectoryIterator(directory);
    }

    private class DirectoryFile implements ModuleFile {

        private final File file;

        DirectoryFile(File file) {
            this.file = file;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public List<String> getPath() {
            try {
                String filePath = file.getParentFile().getCanonicalPath();
                String basePath = directory.getCanonicalPath();

                return Arrays.asList(filePath.substring(basePath.length() + 1).split(Pattern.quote(File.separator )));
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
        public long getSize() {
            return file.length();
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

    private class DirectoryIterator implements Iterator<ModuleFile> {

        private Deque<File> files = Queues.newArrayDeque();
        private ModuleFile next;

        DirectoryIterator(File baseDirectory) {
            files.add(baseDirectory);
            findNext();
        }

        private void findNext() {
            next = null;
            while (next == null && !files.isEmpty()) {
                File file = files.pop();
                if (file.isDirectory()) {
                    File[] contents = file.listFiles();
                    if (contents != null) {
                        files.addAll(Arrays.asList(contents));
                    }
                } else if (file.exists()) {
                    next = new DirectoryFile(file);
                }
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
