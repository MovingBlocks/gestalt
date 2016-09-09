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

package org.terasology.entitysystem.persistence.proto.typehandlers.primitives;

import com.google.common.reflect.TypeToken;
import com.google.protobuf.ByteString;
import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandlerFactory;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;
import org.terasology.reflection.ReflectionUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 */
public class ArrayHandlerFactory implements ProtoTypeHandlerFactory {

    @Override
    public <T> ProtoTypeHandler<T> createTypeHandler(TypeToken<T> type) {
        if (type.isArray()) {
            if (!type.getComponentType().isPrimitive()) {
                return (ProtoTypeHandler<T>) new ArrayHandler<T>(type.getComponentType().getType());
            } else if (type.getComponentType().getType() == Integer.TYPE) {
                return (ProtoTypeHandler<T>) new IntArrayHandler();
            } else if (type.getComponentType().getType() == Float.TYPE) {
                return (ProtoTypeHandler<T>) new FloatArrayHandler();
            } else if (type.getComponentType().getType() == Boolean.TYPE) {
                return (ProtoTypeHandler<T>) new BooleanArrayHandler();
            } else if (type.getComponentType().getType() == Byte.TYPE) {
                return (ProtoTypeHandler<T>) new ByteArrayHandler();
            } else if (type.getComponentType().getType() == Character.TYPE) {
                return (ProtoTypeHandler<T>) new CharArrayHandler();
            } else if (type.getComponentType().getType() == Double.TYPE) {
                return (ProtoTypeHandler<T>) new DoubleArrayHandler();
            } else if (type.getComponentType().getType() == Long.TYPE) {
                return (ProtoTypeHandler<T>) new LongArrayHandler();
            }
        }
        return null;
    }

    private class ArrayHandler<T> implements ProtoTypeHandler<T[]> {

        private Type componentType;

        ArrayHandler(Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public ProtoDatastore.Value.Builder serialize(T[] instance, Type type, ProtoContext context) {
            return context.serializeCollection(Arrays.asList(instance), componentType);
        }

        @Override
        public T[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            Collection<Object> items = context.deserializeCollection(value, componentType);
            T[] o = (T[]) Array.newInstance(ReflectionUtil.getClassOfType(componentType), items.size());
            return items.toArray(o);
        }
    }

    private class IntArrayHandler implements ProtoTypeHandler<int[]> {

        @Override
        public ProtoDatastore.Value.Builder serialize(int[] instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            for (int i : instance) {
                builder.addInteger(i);
            }
            return builder;
        }

        @Override
        public int[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            int[] result = new int[value.getIntegerCount()];
            for (int i = 0; i < value.getIntegerCount(); ++i) {
                result[i] = value.getInteger(i);
            }
            return result;
        }
    }

    private class LongArrayHandler implements ProtoTypeHandler<long[]> {

        @Override
        public ProtoDatastore.Value.Builder serialize(long[] instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            for (long i : instance) {
                builder.addLong(i);
            }
            return builder;
        }

        @Override
        public long[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            long[] result = new long[value.getLongCount()];
            for (int i = 0; i < value.getLongCount(); ++i) {
                result[i] = value.getLong(i);
            }
            return result;
        }
    }

    private class FloatArrayHandler implements ProtoTypeHandler<float[]> {

        @Override
        public ProtoDatastore.Value.Builder serialize(float[] instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            for (float i : instance) {
                builder.addFloat(i);
            }
            return builder;
        }

        @Override
        public float[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            float[] result = new float[value.getFloatCount()];
            for (int i = 0; i < value.getFloatCount(); ++i) {
                result[i] = value.getFloat(i);
            }
            return result;
        }
    }

    private class BooleanArrayHandler implements ProtoTypeHandler<boolean[]> {

        @Override
        public ProtoDatastore.Value.Builder serialize(boolean[] instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            for (boolean i : instance) {
                builder.addBoolean(i);
            }
            return builder;
        }

        @Override
        public boolean[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            boolean[] result = new boolean[value.getBooleanCount()];
            for (int i = 0; i < value.getBooleanCount(); ++i) {
                result[i] = value.getBoolean(i);
            }
            return result;
        }
    }

    private class ByteArrayHandler implements ProtoTypeHandler<byte[]> {

        @Override
        public ProtoDatastore.Value.Builder serialize(byte[] instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            builder.setBytes(ByteString.copyFrom(instance));
            return builder;
        }

        @Override
        public byte[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            return value.getBytes().toByteArray();
        }
    }

    private class CharArrayHandler implements ProtoTypeHandler<char[]> {

        @Override
        public ProtoDatastore.Value.Builder serialize(char[] instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            builder.addString(new String(instance));
            return builder;
        }

        @Override
        public char[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            if (value.getStringCount() > 0) {
                return value.getString(0).toCharArray();
            }
            return new char[0];
        }
    }

    private class DoubleArrayHandler implements ProtoTypeHandler<double[]> {

        @Override
        public ProtoDatastore.Value.Builder serialize(double[] instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            for (double i : instance) {
                builder.addDouble(i);
            }
            return builder;
        }

        @Override
        public double[] deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            double[] result = new double[value.getDoubleCount()];
            for (int i = 0; i < value.getDoubleCount(); ++i) {
                result[i] = value.getDouble(i);
            }
            return result;
        }
    }

}
