// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di;

/**
 * lifetime scope drives how persistent a bean will and how it will be reused between different scopes. a child scope that redfined a bean will shadow its parents even if a
 * parent scope has a singleton objects.
 *
 * - Scoped --> A single instance is defined for every nested scope. (A --> B --> C) a scoped instanced defined at B will create a new single instance in C and B
 * - ScopedToChildren --> A new instance is defined for every nested scope except the one its defined in.   (A --> B --> C --> D) a scoped instanced defined at B will create a new single instance in C and D
 * - Singleton  --> A single instance is defined and visible to all children.
 * - Transient --> A new object is always returned if requested.
 */
public enum Lifetime {
    Scoped,
    ScopedToChildren,
    Singleton,
    Transient
}
