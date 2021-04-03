// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context;

/**
 * lifetime scope drives how persistent a bean will and how it will be reused between different scopes. a child scope that redfined a bean will shadow its parents even if a
 * parent scope has a singleton objects.
 * <p>- Scoped --&gt; A single instance is defined for every nested scope. (A --&gt; B --&gt; C) a scoped instanced defined at B will create a new single instance in C and B</p>
 * <p>- ScopedToChildren --&gt; A new instance is defined for every nested scope except the one its defined in.   (A --&gt; B --&gt; C --&gt; D) a scoped instanced defined at B will create a new single instance in C and D</p>
 * <p>- Singleton --&gt; A single instance is defined and visible to all children.</p>
 * <p>- Transient --&gt; A new object is always returned if requested. </p>
 */
public enum Lifetime {
    Scoped,
    ScopedToChildren,
    Singleton,
    Transient
}
