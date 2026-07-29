plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.plugins.build.health.asDependency())
    implementation(libs.plugins.foojay.resolver.convention.asDependency())
    implementation(libs.plugins.git.hooks.asDependency())
    implementation(libs.plugins.gradle.develocity.asDependency())
    implementation(libs.plugins.japicmp.asDependency())
    implementation(libs.plugins.jreleaser.asDependency())

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

// workaround for https://github.com/gradle/gradle/issues/17963
fun Provider<PluginDependency>.asDependency(): String =
    get().let {
        val id = it.pluginId
        val version = it.version
        return "$id:$id.gradle.plugin:$version"
    }
