// https://github.com/gradle/gradle/issues/30299
rootProject.name = "plugins"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement { versionCatalogs { create("libs") { from(files("../libs.versions.toml")) } } }
