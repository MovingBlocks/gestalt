// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.module.sandbox;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.net.URL;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Immortius
 */
public class APIScannerTest {

    @Test
    public void testAnnotatedClassIsPermitted() {
        StandardPermissionProviderFactory permissionProviderFactory = mock(StandardPermissionProviderFactory.class);
        PermissionSet permSet = new PermissionSet();
        when(permissionProviderFactory.getPermissionSet(any(String.class))).thenReturn(permSet);

        Collection<URL> urls = ClasspathHelper.forJavaClassPath();
        assertFalse(urls.isEmpty(), "Tried to construct test reflections with no URLs.");

        ConfigurationBuilder config =
                new ConfigurationBuilder()
                        .addUrls(urls)
                        .addScanners(new TypeAnnotationsScanner());
        Reflections reflections = new Reflections(config);

        new APIScanner(permissionProviderFactory).scan(reflections);
        assertTrue(permSet.isPermitted(APIClass.class));
        assertFalse(permSet.isPermitted(NonAPIClassInheritingAPIClass.class));
    }
}
