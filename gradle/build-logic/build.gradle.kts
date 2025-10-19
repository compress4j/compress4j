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
    implementation(libs.plugins.gradle.develocity.asDependency())
}

// workaround for https://github.com/gradle/gradle/issues/17963
fun Provider<PluginDependency>.asDependency(): String =
    get().let {
        val id = it.pluginId
        val version = it.version
        return "$id:$id.gradle.plugin:$version"
    }
