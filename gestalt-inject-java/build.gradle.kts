// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("gestalt-library-common")
}

dependencies {
    implementation(project(":gestalt-util"))
    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.slf4j.api)
    implementation(libs.android.annotation)
    implementation("com.github.zafarkhaja:java-semver:0.10.2")

    testImplementation(project(":testpack:testpack-api"))
    testImplementation(libs.junit)
    testImplementation(libs.logback)
    testImplementation(libs.mockito)

    implementation("com.squareup:javapoet:1.13.0")
    implementation("javax.inject:javax.inject:1")
    implementation(project(":gestalt-inject"))
}

// https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#test_task_fails_when_no_tests_are_discovered
tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.withType<ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer> {
    // The gestalt-inject-java annotation processor is only used at compile-time and not intended for use on Android.
    exclude("**/*")
}