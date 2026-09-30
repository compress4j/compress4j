plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.plugins.build.health.asDependencyWorkaround())
    implementation(libs.plugins.foojay.resolver.convention.asDependencyWorkaround())
    implementation(libs.plugins.git.hooks.asDependencyWorkaround())
    implementation(libs.plugins.gradle.develocity.asDependencyWorkaround())
    implementation(libs.plugins.japicmp.asDependencyWorkaround())
    implementation(libs.plugins.jreleaser.asDependencyWorkaround())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

fun Provider<PluginDependency>.asDependencyWorkaround(): String =
    get().let {
        val id = it.pluginId
        val version = it.version
        return "$id:$id.gradle.plugin:$version"
    }
