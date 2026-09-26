// Copyright 2026 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

// Most typical common config, but not quite global
plugins {
    `java-library`
    `maven-publish`
    id("ru.vyarus.animalsniffer")
}

dependencies {
    add("signature", "com.toasttab.android:gummy-bears-api-24:0.15.0:coreLib2@signature")
}

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

extensions.configure<ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension> {
    // java.nio.* APIs can be desugared by D8. java.io.File.toPath() also needs to be excluded.
    ignore = listOf("java.nio.file.*", "java.io.File")
}

// Extra details provided for unit tests
tasks.withType<Test> {
    useJUnit()

    // ignoreFailures: Specifies whether the build should break when the verifications performed by this task fail.
    ignoreFailures = true

    // showStandardStreams: makes the standard streams (err and out) visible at console when running tests
    testLogging.showStandardStreams = true

    reports {
        junitXml.required.set(true)
    }

    // Arguments to include while running tests
    jvmArgs = listOf("-Xms512m", "-Xmx1024m")
}

// Javadoc configuration
tasks.withType<Javadoc> {
    isFailOnError = false
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>(project.name) {
            // Without this we get a .pom with no dependencies
            from(components["java"])

            repositories {
                maven {
                    name = "TerasologyOrg"
                    url = uri(
                        if (rootProject.hasProperty("publishRepo")) {
                            // This first option is good for local testing, you can set a full explicit target repo in gradle.properties
                            "https://artifactory.terasology.io/artifactory/${rootProject.property("publishRepo")}"
                        } else {
                            // Support override from the environment to use a different target publish org
                            val deducedPublishRepo = System.getenv("PUBLISH_ORG").takeIf { it?.isNotEmpty() == true }
                                ?: "libs"

                            val suffix = if (project.version.toString().endsWith("SNAPSHOT")) {
                                "-snapshot-local"
                            } else {
                                "-release-local"
                            }

                            logger.info("The final deduced publish repo is {}", deducedPublishRepo + suffix)
                            "https://artifactory.terasology.io/artifactory/$deducedPublishRepo$suffix"
                        }
                    )

                    if (rootProject.hasProperty("mavenUser") && rootProject.hasProperty("mavenPass")) {
                        credentials {
                            username = rootProject.property("mavenUser").toString()
                            password = rootProject.property("mavenPass").toString()
                        }
                        authentication {
                            create<BasicAuthentication>("basic")
                        }
                    }
                }
            }
        }
    }
}
