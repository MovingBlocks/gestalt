// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

plugins {
    id ("gestalt-library-common")
}

dependencies {
    implementation("org.javassist:javassist:3.27.0-GA") // TODO: REMOVE this. Replace with gestalt's DI generator (used by ByteCodeInjector)

    api(project(":gestalt-di"))

    implementation(project(":gestalt-util"))
    annotationProcessor(project(":gestalt-inject-java"))

    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.slf4j.api)
    implementation(libs.android.annotation)
    implementation("com.github.zafarkhaja:java-semver:0.10.2")

    testImplementation(project(":testpack:testpack-api"))
    testAnnotationProcessor(project(":gestalt-inject-java"))
    testImplementation(libs.junit)
    testImplementation(libs.logback)
    testImplementation(libs.mockito)
}

// Configure Java compilation options
tasks.named<JavaCompile>("compileJava") {
    inputs.files(sourceSets.main.get().resources.srcDirs)
    options.compilerArgs.add("-Aresource=${sourceSets.main.get().resources.srcDirs.joinToString(File.pathSeparator)}")
}

tasks.named<JavaCompile>("compileTestJava") {
    inputs.files(sourceSets.test.get().resources.srcDirs)
    options.compilerArgs.add("-Aresource=${sourceSets.test.get().resources.srcDirs.joinToString(File.pathSeparator)}")
}

// Library and distribution config
description = "Provides support for modules - java libraries that can be activated at runtime and run in a sandboxed environment"

// Task registrations
val gatherJarModules = tasks.register<Copy>("gatherJarModules") {
    dependsOn(":testpack:moduleA:jar", ":testpack:moduleB:jar", ":testpack:moduleC:jar", ":testpack:moduleD:jar")
    from("../testpack/moduleA/build/libs/")
    from("../testpack/moduleB/build/libs/")
    from("../testpack/moduleC/build/libs/")
    from("../testpack/moduleD/build/libs/")
    into("test-modules")
    include("*.jar")
}

val copyModuleELibs = tasks.register<Copy>("copyModuleELibs") {
    dependsOn(":testpack:moduleA:jar", ":testpack:moduleD:jar")
    from("../testpack/moduleA/build/libs")
    from("../testpack/moduleD/build/libs")
    into("test-modules/moduleE/libs")
    include("*.jar")
}

val copyModuleEInfo = tasks.register<Copy>("copyModuleEInfo") {
    from("../testpack/moduleE")
    into("test-modules/moduleE")
    include("*.json")
}

val createModuleE = tasks.register("createModuleE") {
    dependsOn(":gestalt-module:copyModuleEInfo", ":gestalt-module:copyModuleELibs")
}

val gatherModules = tasks.register("gatherModules") {
    dependsOn(":gestalt-module:gatherJarModules", ":gestalt-module:createModuleE")
}

// Make the test task depend on gatherModules
tasks.named("test") {
    dependsOn(gatherModules)
}
