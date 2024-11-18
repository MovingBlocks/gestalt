// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
apply(from = "$rootDir/gradle/common.gradle.kts")

plugins {
    `java-library`
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
val gatherJarModules by tasks.registering(Copy::class) {
    dependsOn(":testpack:moduleF:jar")
    from("../testpack/moduleF/build/libs/")
    into("test-modules")
    include("*.jar")
}

// Register the gatherModules task
val gatherModules by tasks.registering {
    dependsOn(":gestalt-es-perf:gatherJarModules")
}

// Make the test task depend on gatherModules
tasks.named("test") {
    dependsOn(gatherModules)
}
