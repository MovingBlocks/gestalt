// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
apply(from = "$rootDir/gradle/common.gradle.kts")

plugins {
    `java-library`
}

dependencies {
    api("javax.inject:javax.inject:1")
    implementation(libs.slf4j.api)
    implementation(libs.guava)
}
