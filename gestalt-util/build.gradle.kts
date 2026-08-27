// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("gestalt-library-common")
}

// Primary dependencies definition
dependencies {
    implementation(libs.guava)
    implementation("com.googlecode.gentyref:gentyref:1.2.0")
    implementation(libs.slf4j.api)
    implementation(libs.android.annotation)

    // These dependencies are only needed for running tests
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.logback)
    testImplementation(libs.mockito)
}
