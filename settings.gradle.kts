rootProject.name = "gestalt"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("android-annotation", "com.android.support:support-annotations:28.0.0")
            library("gson", "com.google.code.gson:gson:2.8.5")
            library("guava", "com.google.guava:guava:27.0.1-android")
            library("jcip", "net.jcip:jcip-annotations:1.0")
            library("slf4j-api", "org.slf4j:slf4j-api:2.0.18")
            // testing
            library("junit", "junit:junit:4.12")
            library("logback", "ch.qos.logback:logback-classic:1.6.0")
            library("mockito", "org.mockito:mockito-core:5.23.0")
        }
    }
}

pluginManagement {
    includeBuild("build-logic")
}

include(
    "gestalt-util",
    "gestalt-di",
    "gestalt-inject-java",
    "gestalt-inject",
    "testpack:testpack-api",
    "gestalt-module",
    "testpack:moduleA",
    "testpack:moduleB",
    "testpack:moduleC",
    "testpack:moduleD",
    "testpack:moduleF",
    "gestalt-asset-core",
    "gestalt-entity-system",
    "gestalt-es-perf"
)
if (rootProject.projectDir.resolve("local.properties").exists()) {
    include("gestalt-android", "gestalt-android-testbed")
} else {
    println("No local.properties file found, bypassing Android elements")
}
