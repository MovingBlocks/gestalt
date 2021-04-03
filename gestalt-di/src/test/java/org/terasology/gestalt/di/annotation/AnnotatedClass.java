// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.annotation;

import javax.inject.Singleton;

@Singleton
@TestAnnotation
@AnnotationValue(one = "hello world")
public class AnnotatedClass {
}
