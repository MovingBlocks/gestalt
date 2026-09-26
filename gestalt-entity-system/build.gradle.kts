// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("gestalt-library-common")
}

dependencies {
    implementation(project(":gestalt-util"))
    implementation(project(":gestalt-module"))
    implementation(project(":gestalt-asset-core"))
    implementation(project(":gestalt-inject"))
    annotationProcessor(project(":gestalt-inject-java"))

    implementation(libs.guava)
    implementation(libs.slf4j.api)
    implementation(libs.android.annotation)
    implementation(libs.jcip)
    implementation("net.sf.trove4j:trove4j:3.0.3")
    implementation(libs.gson)

    testAnnotationProcessor(project(":gestalt-inject-java"))
    testImplementation(libs.junit)
    testImplementation(libs.logback)
    testImplementation(libs.mockito)
}

tasks.named<JavaCompile>("compileJava") {
    inputs.files(sourceSets.main.get().resources.srcDirs)
    options.compilerArgs.add("-Aresource=${sourceSets.main.get().resources.srcDirs.joinToString(File.pathSeparator)}")
}

tasks.named<JavaCompile>("compileTestJava") {
    inputs.files(sourceSets.test.get().resources.srcDirs)
    options.compilerArgs.add("-Aresource=${sourceSets.test.get().resources.srcDirs.joinToString(File.pathSeparator)}")
}
