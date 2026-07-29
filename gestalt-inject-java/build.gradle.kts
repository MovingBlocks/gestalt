// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
apply(from = "$rootDir/gradle/common.gradle.kts")

plugins {
    `java-library`
}

dependencies {
    implementation(project(":gestalt-util"))
    implementation(libs.guava)
    implementation(libs.gson)
    implementation("org.apache.commons:commons-vfs2:2.2")
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
