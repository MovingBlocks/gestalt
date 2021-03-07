// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

public interface ServiceDefinition<T> {
    String getName();

    T load();
}
