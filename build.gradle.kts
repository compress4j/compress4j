@file:Suppress("UnstableApiUsage")

import com.diffplug.spotless.FormatterFunc
import org.jreleaser.model.Active
import java.io.Serializable

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
        languageVersion = JavaLanguageVersion.of(11)
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

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    api(libs.commons.compress)

    implementation(libs.commons.io)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.slf4j.api)

    testFixturesImplementation(platform(libs.junit.bom))

    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.logback.classic)
    testFixturesApi(libs.logback.core)
    testFixturesImplementation(libs.assertj.core)

    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            dependencies {
                implementation(platform(libs.junit.bom))

                implementation(libs.assertj.core)
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.params)
                implementation(libs.logback.classic)
                implementation(libs.logback.core)
                implementation(libs.mockito.core)
                implementation(libs.mockito.jupiter)
            }
            targets.all { testTask.configure {
                jvmArgs =
                    listOf(
                        "-javaagent:${mockitoAgent.asPath}",
                        "--add-opens=java.base/java.util.zip=ALL-UNNAMED"
                    )
            } }
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(platform(libs.junit.bom))
                implementation(project())
                implementation(testFixtures(project()))

                implementation(libs.assertj.core)
                implementation(libs.junit.jupiter.api)

                runtimeOnly(libs.org.tukaani.xz)
            }

            targets.all {
                    testTask.configure {
                        shouldRunAfter(tasks.test)
                    }
            }
        }
    }
}

dependencyAnalysis {
    issues {
        all {
            onUnusedDependencies {
                exclude("org.junit.jupiter:junit-jupiter")
            }
            onAny {
                severity("fail")
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
    dependsOn(tasks.buildHealth, tasks.testCodeCoverageReport)
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
        custom("Refuse wildcard imports", object : Serializable, FormatterFunc {
            override fun apply(input: String): String {
                if (input.contains("\nimport .*\\*;".toRegex())) {
                    throw AssertionError(
                        "Wildcard imports (e.g., 'import java.util.*;') are not allowed. " +
                                "Please use explicit imports. 'spotlessApply' cannot resolve this issue automatically."
                    )
                }
                return input
            }
        })
    }
    format("javaMisc") {
        target("src/**/package-info.java")
        licenseHeaderFile(rootProject.file(".config/spotless/copyright.java.txt"), "\\/\\*\\*|@Nonnull\\npackage |package ")
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
