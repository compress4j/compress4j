@file:Suppress("UnstableApiUsage")

import org.jreleaser.model.Active

plugins {
    `jacoco-report-aggregation`
    `java-library`
    `java-test-fixtures`
    `maven-publish`
    jacoco

    alias(libs.plugins.git.version)
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.spotless)
}
val stagingDir: Provider<Directory> = layout.buildDirectory.dir("staging-deploy")
val snapshotVersion: String = "\${describe.tag.version.major}." +
        "\${describe.tag.version.minor}." +
        "\${describe.tag.version.patch.next}-SNAPSHOT"

group = "io.github.compress4j"
description = "A simple archiving and compression library for Java."
version = "0.0.0-SNAPSHOT"


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

    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.logback.classic)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.jupiter)

    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(platform(libs.junit.bom))

    testFixturesImplementation(libs.assertj.core)
    testFixturesImplementation(libs.junit.jupiter)
    testFixturesApi(libs.logback.classic)
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
        property("sonar.projectKey", "compress4j_compress4j")
        property("sonar.organization", "compress4j")
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

gitVersioning.apply {
    refs {
        branch("main") {
            version = snapshotVersion
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    // optional fallback configuration in case of no matching ref configuration
    rev {
        version = snapshotVersion
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            val javaComponent = components["java"] as AdhocComponentWithVariants
            javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
            javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
            pom {
                name = project.name
                description = project.description
                url = "https://github.com/compress4j/compress4j"
                scm {
                    connection = "scm:git:https://github.com/compress4j/compress4j.git"
                    developerConnection = "scm:git:git@github.com:compress4j/compress4j.git"
                    url = "https://github.com/compress4j/compress4j.git"
                }
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "austek"
                        name = "Ali Ustek"
                    }
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(stagingDir.get().toString())
        }
    }
}

jreleaser {
    signing {
        active = Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(stagingDir.get().toString())
                }
            }
        }
    }
}
