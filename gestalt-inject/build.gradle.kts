// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("gestalt-library-common")
}

dependencies {
    api("javax.inject:javax.inject:1")
    implementation(libs.slf4j.api)
    implementation(libs.guava)
}
