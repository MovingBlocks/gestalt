// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("gestalt-library-common")
}

dependencies {
    implementation(project(":gestalt-util"))
    implementation(project(":gestalt-module"))
    implementation(project(":gestalt-asset-core"))
    implementation(project(":gestalt-entity-system"))

    implementation(libs.guava)
    implementation(libs.slf4j.api)
    implementation(libs.android.annotation)
    implementation(libs.jcip)

    testImplementation(libs.junit)
    testImplementation(libs.logback)
    testImplementation(libs.mockito)
}

description = "High performance access methods to replace the use of reflections in gestalt-entity-system. Can be used in Java 7+ and Android API 26+."

/***
 * Testpack inclusion
 */

// Register the gatherJarModules task
val gatherJarModules = tasks.register<Copy>("gatherJarModules") {
    dependsOn(":testpack:moduleF:jar")
    from("../testpack/moduleF/build/libs/")
    into("test-modules")
    include("*.jar")
}

// Register the gatherModules task
val gatherModules = tasks.register("gatherModules") {
    dependsOn(":gestalt-es-perf:gatherJarModules")
}

// Make the test task depend on gatherModules
tasks.named("test") {
    dependsOn(gatherModules)
}

tasks.withType<ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer> {
    // The gestalt-es-perf library is not intended for use on Android.
    exclude("**/*")
}
