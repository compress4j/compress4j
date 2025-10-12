@file:Suppress("UnstableApiUsage")

import com.diffplug.spotless.FormatterFunc
import org.gradle.kotlin.dsl.invoke
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

val examples: SourceSet by sourceSets.creating {
    val examplesDir = layout.projectDirectory.dir("docs/modules/ROOT/examples")
    java.srcDir(examplesDir.file("java"))
    resources.srcDir(examplesDir.file("resources"))
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withJavadocJar()
    withSourcesJar()
}

val mockitoAgent: Configuration = configurations.create("mockitoAgent")

dependencies {
    api(libs.commons.compress)
    api(libs.commons.io)
    api(libs.jakarta.annotation.api)

    implementation(libs.commons.lang3)
    implementation(libs.slf4j.api)

    testFixturesApi(libs.assertj.core)
    testFixturesApi(libs.commons.compress)
    testFixturesApi(libs.jackson.core)
    testFixturesApi(libs.jakarta.annotation.api)
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.logback.classic)
    testFixturesApi(libs.logback.core)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.commons.io)
    testFixturesImplementation(libs.jackson.annotations)
    testFixturesImplementation(libs.jackson.databind)
    testFixturesImplementation(libs.mockito.core)
    
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            useJUnitJupiter()
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

    }
}

val integrationTest by testing.suites.registering(JvmTestSuite::class) {
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

val e2eTest by testing.suites.registering(JvmTestSuite::class) {
    dependencies {
        implementation(platform(libs.junit.bom))
        implementation(project())
        implementation(testFixtures(project()))
        implementation(libs.junit.jupiter.api)
    }

    targets.all {
        testTask.configure {
            shouldRunAfter(integrationTest)
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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all")
}

tasks.testCodeCoverageReport {
    dependsOn(tasks.test, integrationTest, e2eTest)
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
        palantirJavaFormat("2.71.0").formatJavadoc(true)
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
            suppressPomMetadataWarningsFor("testFixturesApiElements")
            suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
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
                    developer {
                        id = "renasustek"
                        name = "Renas Ustek"
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
