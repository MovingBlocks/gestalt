// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.di.singleton;

import javax.inject.Singleton;

@Singleton
public class BasicSingleton {

    public String returnString(){
        return "It's works!";
    }
}
