// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
plugins {
    id ("gestalt-library-common")
}

// Primary dependencies definition
dependencies {
    implementation(project(":gestalt-util"))
    implementation(project(":gestalt-module"))
    implementation(project(":gestalt-inject"))
    annotationProcessor(project(":gestalt-inject-java"))

    implementation(libs.guava)
    implementation(libs.slf4j.api)
    implementation(libs.android.annotation)
    api(libs.jcip)

    testAnnotationProcessor(project(":gestalt-inject-java"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.logback)
    testImplementation(libs.mockito)
}

// include resource dirs from main or test, depending if is test compile or not
tasks.withType<JavaCompile>().configureEach {
    val resourceDirs = sourceSets[if (name == "compileTestJava") "test" else "main"].resources.srcDirs
    inputs.files(resourceDirs)
    options.compilerArgs.addAll(
        listOf("-Aresource=${resourceDirs.joinToString(File.pathSeparator)}")
    )
}