// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.beans;

import org.terasology.context.annotation.Introspected;

@Introspected
public class NestedClassDep {
    enum Value {
        One,
        Two,
        Three
    }

    @Introspected
    public static class InternalDep {

    }

    public static class InternalDepNonIntrospected {

    }
}
