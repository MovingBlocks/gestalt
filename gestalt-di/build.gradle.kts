// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("gestalt-library-common")
}

tasks.register<Copy>("gatherJarModules") {
    dependsOn(
        ":testpack:moduleA:jar",
        ":testpack:moduleB:jar",
        ":testpack:moduleC:jar",
        ":testpack:moduleD:jar"
    )

    from("../testpack/moduleA/build/libs/")
    from("../testpack/moduleB/build/libs/")
    from("../testpack/moduleC/build/libs/")
    from("../testpack/moduleD/build/libs/")
    from("../testpack/moduleF/build/libs/")
    into("test-modules")
    include("*.jar")
}

// Primary dependencies definition
dependencies {
    testAnnotationProcessor(project(":gestalt-inject-java"))

    implementation(libs.slf4j.api)
    implementation(libs.guava)
    api(project(":gestalt-inject"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.logback)
    testImplementation(libs.mockito)

    testImplementation(project(":gestalt-module"))
    testImplementation(project(":testpack:testpack-api"))
    testImplementation(project(":gestalt-entity-system"))
}

tasks.named("test") {
    dependsOn("gatherJarModules")
}
