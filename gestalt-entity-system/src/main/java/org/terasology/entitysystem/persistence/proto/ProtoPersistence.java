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

package org.terasology.entitysystem.persistence.proto;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitysystem.persistence.proto.exception.PersistenceException;
import org.terasology.entitysystem.persistence.proto.typehandlers.StringHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.collections.ListHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.collections.MapHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.collections.SetHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.ArrayHandlerFactory;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.BooleanHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.ByteHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.CharHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.DoubleHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.FloatHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.IntegerHandler;
import org.terasology.entitysystem.persistence.proto.typehandlers.primitives.LongHandler;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.util.reflection.GenericsUtil;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 */
public class ProtoPersistence implements ProtoContext {

    private static final Logger logger = LoggerFactory.getLogger(ProtoPersistence.class);

    private Map<Type, ProtoTypeHandler<?>> typeTypeHandlerLookup = Maps.newHashMap();
    private Map<Class, ProtoTypeHandler<?>> classTypeHandlerLookup = Maps.newHashMap();
    private List<ProtoTypeHandlerFactory> typeHandlerFactories = Lists.newArrayList();
    private Map<String, BiMap<Integer, String>> lookupTables = Maps.newLinkedHashMap();

    public ProtoPersistence() {
        addTypeHandlerFactory(new ArrayHandlerFactory());
        addTypeHandler(new StringHandler(), String.class);
        addTypeHandler(new ListHandler(), List.class);
        addTypeHandler(new MapHandler(), Map.class);
        addTypeHandler(new SetHandler(), Set.class);
        ProtoTypeHandler booleanTypeHandler = new BooleanHandler();
        addTypeHandler(booleanTypeHandler, Boolean.class);
        addTypeHandler(booleanTypeHandler, Boolean.TYPE);
        ProtoTypeHandler byteTypeHandler = new ByteHandler();
        addTypeHandler(byteTypeHandler, Byte.class);
        addTypeHandler(byteTypeHandler, Byte.TYPE);
        ProtoTypeHandler charHandler = new CharHandler();
        addTypeHandler(charHandler, Character.class);
        addTypeHandler(charHandler, Character.TYPE);
        ProtoTypeHandler doubleHandler = new DoubleHandler();
        addTypeHandler(doubleHandler, Double.class);
        addTypeHandler(doubleHandler, Double.TYPE);
        ProtoTypeHandler floatHandler = new FloatHandler();
        addTypeHandler(floatHandler, Float.class);
        addTypeHandler(floatHandler, Float.TYPE);
        ProtoTypeHandler intHandler = new IntegerHandler();
        addTypeHandler(intHandler, Integer.class);
        addTypeHandler(intHandler, Integer.TYPE);
        ProtoTypeHandler longHandler = new LongHandler();
        addTypeHandler(longHandler, Long.class);
        addTypeHandler(longHandler, Long.TYPE);
    }

    public <T> void addTypeHandler(ProtoTypeHandler<T> handler, Type type) {
        if (type instanceof Class) {
            classTypeHandlerLookup.put((Class) type, handler);
        } else {
            typeTypeHandlerLookup.put(type, handler);
        }
    }

    public <T> void addTypeHandler(ProtoTypeHandler<T> handler, Class type) {
        classTypeHandlerLookup.put(type, handler);
    }

    public void addTypeHandlerFactory(ProtoTypeHandlerFactory factory) {
        typeHandlerFactories.add(factory);
    }

    private ProtoTypeHandler getTypeHandler(Type type) {
        ProtoTypeHandler handler = typeTypeHandlerLookup.get(type);
        if (handler == null) {
            Class<?> classOfType = GenericsUtil.getClassOfType(type);
            if (classOfType == null) {
                throw new PersistenceException("Unable to determine class of type '" + type + "'");
            }
            handler = classTypeHandlerLookup.get(classOfType);
        }
        if (handler == null) {
            handler = generateHandlerFromFactory(type);
        }
        if (handler == null) {
            throw new PersistenceException("No type handler registered able to support '" + type + "'");
        }
        return handler;
    }

    private ProtoTypeHandler generateHandlerFromFactory(Type type) {
        for (ProtoTypeHandlerFactory typeHandlerFactory : typeHandlerFactories) {
            ProtoTypeHandler handler = typeHandlerFactory.createTypeHandler(TypeToken.of(type));
            if (handler != null) {
                typeTypeHandlerLookup.put(type, handler);
                return handler;
            }
        }
        return null;
    }

    @Override
    public <T> T deserialize(ProtoDatastore.Value value, Class<T> type) {
        return type.cast(getTypeHandler(type).deserialize(value, type, this));
    }

    @Override
    public <T> T deserialize(ProtoDatastore.Value value, Type type) {
        return (T) getTypeHandler(type).deserialize(value, type, this);
    }

    @Override
    public <T> List<T> deserializeCollection(ProtoDatastore.Value value, Class<T> type) {
        return getTypeHandler(type).deserializeCollection(value, type, this);
    }

    @Override
    public <T> List<T> deserializeCollection(ProtoDatastore.Value value, Type type) {
        return getTypeHandler(type).deserializeCollection(value, type, this);
    }

    @Override
    public ProtoDatastore.Value.Builder serialize(Object value) {
        return serialize(value, value.getClass());
    }

    @Override
    public ProtoDatastore.Value.Builder serialize(Object value, Type type) {
        return getTypeHandler(type).serialize(value, type, this);
    }

    @Override
    public ProtoDatastore.Value.Builder serializeCollection(Collection<Object> value, Type type) {
        if (value != null) {
            return getTypeHandler(type).serializeCollection(value, type, this);
        } else {
            return ProtoDatastore.Value.newBuilder();
        }
    }

    @Override
    public int getIdFor(String tableId, String value) {
        BiMap<Integer, String> table = getTable(tableId);
        Integer id = table.inverse().get(value);
        if (id == null) {
            id = table.size();
            table.put(id, value);
        }
        return id;
    }

    private BiMap<Integer, String> getTable(String tableId) {
        BiMap<Integer, String> table = lookupTables.get(tableId);
        if (table == null) {
            table = HashBiMap.create();
            lookupTables.put(tableId, table);
        }
        return table;
    }

    @Override
    public String getValueFor(String tableId, int id) {
        BiMap<Integer, String> table = getTable(tableId);
        String result = table.get(id);
        if (result == null) {
            throw new PersistenceException("No entry for id '" + id + "' in lookup table '" + tableId + "'");
        }
        return result;
    }

    @Override
    public BiMap<Integer, String> getLookupTable(String tableId) {
        return ImmutableBiMap.copyOf(getTable(tableId));
    }

    @Override
    public Collection<String> getLookupTableIds() {
        return lookupTables.keySet();
    }

    /**
     * Sets a lookup table
     * @param tableId
     * @param table
     */
    public void setLookupTable(String tableId, BiMap<Integer, String> table) {
        lookupTables.put(tableId, HashBiMap.create(table));
    }

}
