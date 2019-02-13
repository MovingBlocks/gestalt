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

package org.terasology.entitysystem.persistence.proto.typehandlers;

import com.google.common.reflect.TypeToken;

import org.terasology.assets.Asset;
import org.terasology.assets.management.AssetManager;
import org.terasology.entitysystem.persistence.proto.ProtoContext;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandler;
import org.terasology.entitysystem.persistence.proto.ProtoTypeHandlerFactory;
import org.terasology.entitysystem.persistence.protodata.ProtoDatastore;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 *
 */
public class AssetTypeHandlerFactory implements ProtoTypeHandlerFactory {

    private final AssetManager assetManager;

    public AssetTypeHandlerFactory(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public <T> ProtoTypeHandler<T> createTypeHandler(TypeToken<T> type) {
        if (Asset.class.isAssignableFrom(type.getRawType())) {
            return (ProtoTypeHandler<T>) new AssetTypeHandler(type.getRawType(), assetManager);
        }
        return null;
    }

    private static class AssetTypeHandler<T extends Asset> implements ProtoTypeHandler<T> {

        private Class<T> assetType;
        private AssetManager assetManager;

        public AssetTypeHandler(Class<T> type, AssetManager assetManager) {
            this.assetType = type;
            this.assetManager = assetManager;
        }

        @Override
        public ProtoDatastore.Value.Builder serialize(T instance, Type type, ProtoContext context) {
            ProtoDatastore.Value.Builder builder = ProtoDatastore.Value.newBuilder();
            builder.addString(instance.getUrn().toString());
            return builder;
        }

        @Override
        public T deserialize(ProtoDatastore.Value value, Type type, ProtoContext context) {
            if (value.getStringCount() > 0) {
                Optional<T> result = assetManager.getAsset(value.getString(0), assetType);
                return result.orElse(null);
            }
            return null;
        }
    }
}
