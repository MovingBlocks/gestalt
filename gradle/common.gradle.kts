// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

// Most typical common config, but not quite global
apply(plugin = "java-library")
apply(plugin = "maven-publish")

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
