// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.injection;

import java.lang.annotation.Annotation;
import java.util.Objects;

public class StereotypeQualifier<T extends Annotation> implements Qualifier<T> {

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
