// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

// Only needed for the two Android modules as of April 2021
buildscript {
    repositories {
        mavenCentral()

        // Needed for the Android Gradle Plugin to work
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.1")
    }
}

plugins {
    id("idea")
}

allprojects {
    repositories {
        google()
        mavenCentral()

        // Terasology Artifactory instance for libs not readily available elsewhere plus our own libs
        maven {
            val repoViaEnv = System.getenv("RESOLUTION_REPO")
            if (rootProject.hasProperty("alternativeResolutionRepo")) {
                // If the user supplies an alternative repo via gradle.properties then use that
                name = "from alternativeResolutionRepo property"
                url = uri(rootProject.property("alternativeResolutionRepo") as String)
            } else if (!repoViaEnv.isNullOrEmpty()) {
                name = "from \$RESOLUTION_REPO"
                url = uri(repoViaEnv)
            } else {
                // Our default is the main virtual repo containing everything except repos for testing Artifactory itself
                name = "Terasology Artifactory"
                url = uri("https://artifactory.terasology.io/artifactory/virtual-repo-live")
            }
        }

        // SemVer lib
        maven {
            url = uri("https://heisluft.de/maven")
        }
    }
}

