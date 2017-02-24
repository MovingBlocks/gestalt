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
 * Provides support for determining a compatible set of modules, based on what modules and versions of those modules are available, the desired set of modules, and
 * module dependencies.
 *
 * Important classes:
 * <ul>
 *     <li>{@link org.terasology.module.dependencyresolution.DependencyResolver} is the core class for resolving a compatible dependent set</li>
 *     <li>{@link org.terasology.module.dependencyresolution.ResolutionResult} holds the results of dependency resolution</li>
 * </ul>
 */
package org.terasology.module.dependencyresolution;
