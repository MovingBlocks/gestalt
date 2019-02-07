/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.assets.module;

import com.google.common.collect.Lists;

import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleFactory;
import org.terasology.module.sandbox.PermitAllPermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.util.Varargs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class TestModulesUtil {

    private static final String VIRTUAL_MODULE_ROOT = "virtualModules";

    private static final List<String> VIRTUAL_MODULES = Lists.newArrayList(
            "test",
            "deltaA",
            "moduleA",
            "moduleB",
            "overrideA",
            "overrideB",
            "overrideC",
            "overrideD",
            "overrideE",
            "overrideSupplement",
            "overrideWithSupplementOnly",
            "redirectA",
            "supplementA"
    );


    private TestModulesUtil() {

    }

    public static ModuleEnvironment createFullEnvironment() {
        ModuleFactory factory = new ModuleFactory();
        return new ModuleEnvironment(VIRTUAL_MODULES.stream().map(x -> factory.createPackageModule(VIRTUAL_MODULE_ROOT + "." + x)).collect(Collectors.toList()), new PermitAllPermissionProviderFactory());
    }

    public static ModuleEnvironment createEmptyEnvironment() {
        return new ModuleEnvironment(Collections.emptyList(), new PermitAllPermissionProviderFactory());
    }

    public static ModuleEnvironment createEnvironment() {
        return createEnvironment("test");
    }

    public static ModuleEnvironment createEnvironment(String ... modules) {
        return createEnvironment(Arrays.asList(modules));
    }

    public static ModuleEnvironment createEnvironment(List<String> modules) {
        ModuleFactory factory = new ModuleFactory();
        return new ModuleEnvironment(modules.stream().map(x -> factory.createPackageModule(VIRTUAL_MODULE_ROOT + "." + x)).collect(Collectors.toList()), new PermitAllPermissionProviderFactory());
    }

    public static ModuleEnvironment createEnvironment(Name... modules) {
        ModuleFactory factory = new ModuleFactory();
        return new ModuleEnvironment(Arrays.stream(modules).map(x -> factory.createPackageModule(VIRTUAL_MODULE_ROOT + "." + x)).collect(Collectors.toList()), new PermitAllPermissionProviderFactory());
    }
}
