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

package org.terasology.module.sandbox;

import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.terasology.module.Module;
import org.terasology.module.ModuleFactory;
import org.terasology.module.ModuleMetadata;
import org.terasology.naming.Name;
import org.terasology.naming.Version;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class APIScannerTest {

    @Test
    public void test() throws Exception {
        StandardPermissionProviderFactory permissionProviderFactory = mock(StandardPermissionProviderFactory.class);
        PermissionSet permSet = new PermissionSet();
        when(permissionProviderFactory.getPermissionSet(any(String.class))).thenReturn(permSet);

        ConfigurationBuilder config = new ConfigurationBuilder().addClassLoader(ClasspathHelper.contextClassLoader()).addUrls(ClasspathHelper.forClassLoader()).addScanners(new TypeAnnotationsScanner());
        Reflections reflections = new Reflections(config);

        new APIScanner(permissionProviderFactory).scan(reflections);
        assertTrue(permSet.isPermitted(APIClass.class));
        assertFalse(permSet.isPermitted(NonAPIClassInheritingAPIClass.class));
    }
}
