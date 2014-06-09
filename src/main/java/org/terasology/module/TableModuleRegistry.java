/*
 * Copyright 2014 MovingBlocks
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

package org.terasology.module;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of ModuleRegistry based around a com.google.common.collect.Table.
 *
 * @author Immortius
 */
public class TableModuleRegistry implements ModuleRegistry {

    private static final Logger logger = LoggerFactory.getLogger(TableModuleRegistry.class);

    private final Table<Name, Version, Module> modules = HashBasedTable.create();
    private final Map<Name, Module> latestModules = Maps.newHashMap();

    @Override
    public boolean add(Module module) {
        Preconditions.checkNotNull(module);
        if (!modules.contains(module.getId(), module.getVersion())) {
            modules.put(module.getId(), module.getVersion(), module);
            Module previousLatest = latestModules.get(module.getId());
            if (previousLatest == null || previousLatest.getVersion().compareTo(module.getVersion()) <= 0) {
                latestModules.put(module.getId(), module);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Module) {
            Module module = (Module) o;
            if (modules.remove(module.getId(), module.getVersion()) != null) {
                Module latest = latestModules.get(module.getId());
                if (latest.getVersion().compareTo(module.getVersion()) == 0) {
                    updateLatestFor(module.getId());
                }

                return true;
            }
            return false;
        }
        return false;
    }

    private void updateLatestFor(Name moduleId) {
        Module newLatest = null;
        for (Module remainingModule : modules.row(moduleId).values()) {
            if (newLatest == null || remainingModule.getVersion().compareTo(newLatest.getVersion()) > 0) {
                newLatest = remainingModule;
            }
        }
        if (newLatest != null) {
            latestModules.put(moduleId, newLatest);
        } else {
            latestModules.remove(moduleId);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends Module> c) {
        boolean result = false;
        for (Object o : c) {
            if (o instanceof Module) {
                result |= add((Module) o);
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Set<Module> modulesToRetain = Sets.newHashSet();
        for (Object o : c) {
            if (o instanceof Module) {
                modulesToRetain.add((Module) o);
            }
        }

        boolean changed = false;
        Iterator<Module> moduleIterator = modules.values().iterator();
        while (moduleIterator.hasNext()) {
            Module next = moduleIterator.next();
            if (!modulesToRetain.contains(next)) {
                moduleIterator.remove();
                changed = true;
            }
        }

        if (changed) {
            for (Name name : modules.rowKeySet()) {
                updateLatestFor(name);
            }
        }
        return changed;
    }

    @Override
    public Set<Name> getModuleIds() {
        return Sets.newLinkedHashSet(modules.rowKeySet());
    }

    @Override
    public Collection<Module> getModuleVersions(Name id) {
        return Collections.unmodifiableCollection(modules.row(id).values());
    }

    @Override
    public Module getLatestModuleVersion(Name id) {
        return latestModules.get(id);
    }

    @Override
    public Module getLatestModuleVersion(Name id, Version minVersion, Version maxVersion) {
        Module module = latestModules.get(id);
        if (module != null) {
            if (module.getVersion().compareTo(maxVersion) < 0 && module.getVersion().compareTo(minVersion) >= 0) {
                return module;
            }
            if (module.getVersion().compareTo(minVersion) >= 0) {
                Module result = null;
                for (Map.Entry<Version, Module> item : modules.row(id).entrySet()) {
                    if (item.getKey().compareTo(minVersion) >= 0 && item.getKey().compareTo(maxVersion) < 0) {
                        if (result == null || item.getKey().compareTo(result.getVersion()) > 0) {
                            result = item.getValue();
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }

    @Override
    public Module getModule(Name moduleId, Version version) {
        return modules.get(moduleId, version);
    }

    @Override
    public int size() {
        return modules.size();
    }

    @Override
    public boolean isEmpty() {
        return modules.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Module) {
            Module module = (Module) o;
            return modules.contains(module.getId(), module.getVersion());
        }
        return false;
    }

    @Override
    public Iterator<Module> iterator() {
        return Iterators.unmodifiableIterator(modules.values().iterator());
    }

    @Override
    public Object[] toArray() {
        return modules.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return modules.values().toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        modules.clear();
        latestModules.clear();
    }
}
