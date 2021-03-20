// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.context.injection;

import java.util.Objects;

/**
 * A annotation that extends {@link Qualifier} and tied to a bean is used help qualify a candidate when injecting.
 * @param <T>
 */
public class StereotypeQualifier<T extends javax.inject.Qualifier> implements Qualifier<T> {

    private final Class<T> stereotype;

    public StereotypeQualifier(Class<T> stereotype){
        this.stereotype = stereotype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StereotypeQualifier<?> that = (StereotypeQualifier<?>) o;
        return Objects.equals(stereotype, that.stereotype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stereotype);
    }
}
