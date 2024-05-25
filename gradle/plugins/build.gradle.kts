plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.sonarqube.gradle.plugin)
}
