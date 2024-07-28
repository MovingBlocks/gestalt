rootProject.name = "gestalt"
include(
    "gestalt-util",
    "gestalt-di",
    "gestalt-inject-java",
    "gestalt-inject",
    "gestalt-annotation",
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
