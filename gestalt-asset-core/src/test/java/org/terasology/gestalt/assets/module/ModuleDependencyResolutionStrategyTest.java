// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.assets.module;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.naming.Name;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Immortius
 */
public class ModuleDependencyResolutionStrategyTest {

    private static final Name TEST_MODULE = new Name("test");
    private static final Name MODULE_A = new Name("moduleA");

    @Test
    public void resolveNoOptions() {
        ModuleEnvironment environment = TestModulesUtil.createEnvironment();
        ModuleDependencyResolutionStrategy strategy = new ModuleDependencyResolutionStrategy(environment);
        assertEquals(Collections.<Name>emptySet(), strategy.resolve(Collections.<Name>emptySet(), Name.EMPTY));
    }

    @Test
    public void resolveSingleOptionOutsideOfContextAndDependencies() {
        ModuleEnvironment environment = TestModulesUtil.createEnvironment();
        ModuleDependencyResolutionStrategy strategy = new ModuleDependencyResolutionStrategy(environment);
        assertEquals(Collections.<Name>emptySet(), strategy.resolve(ImmutableSet.of(new Name("Cats")), Name.EMPTY));
    }

    @Test
    public void resolveReturnsContextIfPresent() {
        ModuleEnvironment environment = TestModulesUtil.createEnvironment(TEST_MODULE, MODULE_A);
        ModuleDependencyResolutionStrategy strategy = new ModuleDependencyResolutionStrategy(environment);
        assertEquals(ImmutableSet.of(TEST_MODULE), strategy.resolve(ImmutableSet.of(TEST_MODULE, MODULE_A), TEST_MODULE));
    }

    @Test
    public void resolveReturnsOnlyDependenciesOfContext() {
        ModuleEnvironment environment = TestModulesUtil.createEnvironment(TEST_MODULE, MODULE_A);
        ModuleDependencyResolutionStrategy strategy = new ModuleDependencyResolutionStrategy(environment);
        assertEquals(ImmutableSet.of(TEST_MODULE), strategy.resolve(ImmutableSet.of(TEST_MODULE, new Name("Cat")), MODULE_A));
    }

}
