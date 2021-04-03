// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.annotation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@SubAnnotation
@AnnotationValue(one = "five")
@Retention(RUNTIME)
public @interface TestAnnotation {
}
