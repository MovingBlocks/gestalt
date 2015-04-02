/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.module.filesystem;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A watch service for ModuleFileSystems. Wraps a watch service from the default filesystem, and allows keys to be produced that encompass multiple real locations.
 *
 * @author Immortius
 */
class ModuleWatchService implements WatchService {

    private final WatchService defaultWatchService;
    private final ModuleFileSystem fileSystem;
    private final Map<WatchKey, ModuleWatchKey> keyLookup = Maps.newHashMap();

    ModuleWatchService(ModuleFileSystem fileSystem) throws IOException {
        this.fileSystem = fileSystem;
        this.defaultWatchService = FileSystems.getDefault().newWatchService();
    }

    @Override
    public void close() throws IOException {
        defaultWatchService.close();
        keyLookup.clear();
    }

    @Override
    public WatchKey poll() {
        WatchKey polledKey = defaultWatchService.poll();
        if (polledKey != null) {
            synchronized (keyLookup) {
                return keyLookup.get(polledKey);
            }
        }
        return null;
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
        WatchKey polledKey = defaultWatchService.poll(timeout, unit);
        if (polledKey != null) {
            synchronized (keyLookup) {
                return keyLookup.get(polledKey);
            }
        }
        return null;
    }

    @Override
    public WatchKey take() throws InterruptedException {
        WatchKey polledKey = defaultWatchService.take();
        if (polledKey != null) {
            synchronized (keyLookup) {
                return keyLookup.get(polledKey);
            }
        }
        return null;
    }

    WatchKey register(ModulePath path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        if (!fileSystem.equals(path.getFileSystem())) {
            throw new IllegalArgumentException("Path and WatchService belong to different file systems: '" + path.getFileSystem() + "' != '" + fileSystem + "'");
        }
        ModuleWatchKey result = new ModuleWatchKey(path, events, modifiers);
        synchronized (keyLookup) {
            for (WatchKey key : result.realKeys) {
                keyLookup.put(key, result);
            }
        }
        return result;
    }

    void clearInvalidKeys() {
        synchronized (keyLookup) {
            Iterator<Map.Entry<WatchKey, ModuleWatchKey>> iterator = keyLookup.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<WatchKey, ModuleWatchKey> entry = iterator.next();
                if (!entry.getKey().isValid()) {
                    iterator.remove();
                }
            }
        }
    }

    private class ModuleWatchKey implements WatchKey {

        private List<WatchKey> realKeys = Lists.newArrayList();
        private ModulePath path;
        private WrapWatchEvent wrapper = new WrapWatchEvent<>();

        public ModuleWatchKey(ModulePath path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier[] modifiers) throws IOException {
            this.path = path;
            try {
                for (Path realPath : path.getUnderlyingPaths()) {
                    if (realPath.getFileSystem().equals(FileSystems.getDefault())) {
                        WatchKey realKey = realPath.register(defaultWatchService, events, modifiers);
                        realKeys.add(realKey);
                    }
                }
            } catch (IOException e) {
                throw new IOException("Error establishing WatchKey for " + path, e);
            }
        }

        @Override
        public boolean isValid() {
            for (WatchKey key : realKeys) {
                if (key.isValid()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<WatchEvent<?>> pollEvents() {
            List<WatchEvent<?>> events = Lists.newArrayList();
            for (WatchKey key : realKeys) {
                if (key.isValid()) {
                    events.addAll(Collections2.transform(key.pollEvents(), wrapper));
                }
            }
            return events;
        }

        @Override
        public boolean reset() {
            if (isValid()) {
                for (WatchKey key : realKeys) {
                    key.reset();
                }
                return true;
            }
            return false;
        }

        @Override
        public void cancel() {
            Iterator<WatchKey> iterator = realKeys.iterator();
            while (iterator.hasNext()) {
                WatchKey next = iterator.next();
                next.cancel();
                iterator.remove();
            }
            clearInvalidKeys();

        }

        @Override
        public Watchable watchable() {
            return path;
        }

        private class WrapWatchEvent<T> implements Function<WatchEvent<T>, WatchEvent<T>> {

            @Nullable
            @Override
            public WatchEvent<T> apply(@Nullable WatchEvent<T> input) {
                return new ModuleWatchEvent<>(path, input);
            }
        }

    }


    private static class ModuleWatchEvent<T> implements WatchEvent<T> {

        private ModulePath modulePath;
        private WatchEvent<T> event;

        public ModuleWatchEvent(ModulePath modulePath, WatchEvent<T> event) {
            this.modulePath = modulePath;
            this.event = event;
        }

        @Override
        public Kind<T> kind() {
            return event.kind();
        }

        @Override
        public int count() {
            return event.count();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T context() {
            T innerContext = event.context();
            if (innerContext instanceof Path) {
                Path result = modulePath;
                for (Path part : ((Path) innerContext)) {
                    result = result.resolve(part.toString());
                }
                return (T) result;
            } else {
                return innerContext;
            }
        }
    }

}
