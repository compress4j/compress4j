@file:Suppress("UnstableApiUsage")

plugins {
    `jacoco-report-aggregation`
    `java-library`
    `java-test-fixtures`
    jacoco

    alias(libs.plugins.sonarqube)
    alias(libs.plugins.spotless)

    id("compress4j.publishing")
}

group = "org.compress4j"
version = "1.3.0-SNAPSHOT"
description = "A simple archiving and compression library for Java."

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

dependencies {
    api(libs.jakarta.annotation.api)
    api(libs.org.apache.commons.commons.compress)

    testImplementation(platform(libs.junit.bom))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.jupiter)

    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(platform(libs.junit.bom))

    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.assertj.core)
}

tasks.test {
    useJUnitPlatform()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))

                implementation(platform(libs.junit.bom))

                implementation(libs.org.tukaani.xz)
                implementation(libs.assertj.core)
                implementation(libs.junit.jupiter)
                implementation(libs.junit.jupiter.params)

                runtimeOnly(libs.junit.platform.launcher)
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

 tasks.testCodeCoverageReport {
    dependsOn(tasks.test, tasks.named<Test>("integrationTest"))
     executionData(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
    reports {
        xml.required = true
        html.required = true
    }
    mustRunAfter(tasks.spotlessApply, tasks.javadoc)
}

tasks.check {
    dependsOn(tasks.testCodeCoverageReport)
}

sonar {
    properties {
        property("sonar.projectKey", "austek_compress4j")
        property("sonar.organization", "austek")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.exclusions",
            listOf(
                "docs/**/*",
                "**/*Exception.java"
            )
        )
    }
}

tasks.sonar {
    dependsOn(tasks.check)
}

spotless {
    ratchetFrom("origin/main")
    java {
        toggleOffOn()
        palantirJavaFormat("2.47.0").formatJavadoc(true)
        licenseHeaderFile(rootProject.file(".config/spotless/copyright.java.txt"))
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
