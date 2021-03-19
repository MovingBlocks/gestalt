// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.beans;

import org.terasology.context.annotation.Service;
import org.terasology.gestalt.di.BeanContext;

import javax.inject.Inject;

@Service
public class ContextDep {
    @Inject
    public BeanContext context;
}
