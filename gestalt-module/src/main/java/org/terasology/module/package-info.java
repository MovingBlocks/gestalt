/*
 * Copyright 2015 MovingBlocks
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

/**
 * Provides a system for defining and loading "modules" providing code and content. These modules can be loaded and unloaded at runtime. This provides the core of a plugin
 * system.
 *
 * Important classes:
 * <ul>
 *     <li>ModuleRegistry provides a collection for enumerating modules</li>
 *     <li>ModulePathScanner is used to scan a file location for modules</li>
 *     <li>ClasspathModule is used to create module(s) for the libraries on the classpath</li>
 *     <li>DependencyResolver can be used to determine a compatible set of modules from multiple available versions</li>
 *     <li>ModuleEnvironment allows a set of compatible modules to be actively used</li>
 * </ul>
 */
package org.terasology.module;
