@file:Suppress("UnstableApiUsage")

import com.diffplug.spotless.FormatterFunc
import io.github.compress4j.semver.CheckApiCompatibilityTask
import me.champeau.gradle.japicmp.JapicmpTask
import org.jreleaser.model.Active
import java.io.Serializable

plugins {
    `jacoco-report-aggregation`
    `java-library`
    `java-test-fixtures`
    `jvm-test-suite`
    `maven-publish`
    jacoco

    alias(libs.plugins.git.version)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.spotless)
    id("publishing-conventions")
    id("semver-conventions")
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

val xzSupport: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}
val examples: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    compileClasspath += xzSupport.output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withJavadocJar()
    withSourcesJar()
    registerFeature("xzSupport") {
        usingSourceSet(xzSupport)
        capability("${project.group}", "${project.name}-xz-support", "${project.version}")
        withJavadocJar()
        withSourcesJar()
    }
}

val examplesImplementation: Configuration by configurations
val mockitoAgent: Configuration = configurations.create("mockitoAgent")
val xzSupportApi: Configuration by configurations

dependencies {
    api(libs.commons.compress)
    api(libs.commons.io)
    api(libs.jakarta.annotation.api)

    implementation(libs.commons.lang3)
    implementation(libs.slf4j.api)

    testFixturesApi(platform(libs.jackson.bom))
    testFixturesApi(libs.assertj.core)
    testFixturesApi(libs.commons.compress)
    testFixturesApi(libs.jackson.core)
    testFixturesApi(libs.jakarta.annotation.api)
    testFixturesApi(libs.logback.classic)
    testFixturesApi(libs.logback.core)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.commons.io)
    testFixturesImplementation(libs.jackson.annotations)
    testFixturesImplementation(libs.jackson.databind)
    testFixturesImplementation(libs.mockito.core)

    xzSupportApi(libs.org.tukaani.xz)

    examplesImplementation(libs.org.tukaani.xz)

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
        }
    }
}

val xzSupportTest by testing.suites.registering(JvmTestSuite::class) {
    dependencies {
        implementation(platform(libs.junit.bom))
        implementation(project())
        implementation(testFixtures(project()))
        implementation(project()) {
            capabilities {
                requireCapability("${project.group}:${project.name}-xz-support")
            }
        }

        implementation(libs.assertj.core)
        implementation(libs.junit.jupiter.api)
        implementation(libs.mockito.core)
    }

    targets.all { testTask.configure {
        shouldRunAfter(tasks.test)
    }}
}

val integrationTest by testing.suites.registering(JvmTestSuite::class) {
    dependencies {
        implementation(platform(libs.junit.bom))
        implementation(project())
        implementation(testFixtures(project()))
        implementation(project()) {
            capabilities {
                requireCapability("${project.group}:${project.name}-xz-support")
            }
        }

        implementation(libs.junit.jupiter.api)

        runtimeOnly(libs.asm)
    }

    targets.all { testTask.configure {
        shouldRunAfter(xzSupportTest)
    }}
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "-javaagent:${mockitoAgent.asPath}",
            "--add-opens=java.base/java.util.zip=ALL-UNNAMED"
        )
    })
}

val apiBaselineVersion: String = providers.gradleProperty("api.baseline").orElse(semver.previousVersion).get()

// A named configuration would resolve back to the project being built, a detached one honours the coordinates.
fun baselineArtifacts(classifier: String?): FileCollection = when {
    apiBaselineVersion.isEmpty() -> files()
    else -> configurations.detachedConfiguration(
        dependencies.create(
            listOfNotNull("${project.group}", project.name, apiBaselineVersion, classifier).joinToString(":") + "@jar"
        )
    ).apply { isTransitive = false }
}

fun registerApiComparison(name: String, baseline: FileCollection, jarTask: TaskProvider<Jar>, classpath: FileCollection) =
    tasks.register<JapicmpTask>(name) {
        onlyIf { apiBaselineVersion.isNotEmpty() }
        oldArchives.from(baseline)
        newArchives.from(jarTask)
        oldClasspath.from(classpath)
        newClasspath.from(classpath)
        accessModifier = "protected"
        onlyModified = true
        ignoreMissingClasses = true
        xmlOutputFile = layout.buildDirectory.file("reports/japicmp/$name.xml")
        htmlOutputFile = layout.buildDirectory.file("reports/japicmp/$name.html")
    }

val japicmpMain = registerApiComparison(
    "japicmpMain",
    baselineArtifacts(null),
    tasks.jar,
    sourceSets.main.get().compileClasspath
)
val japicmpXzSupport = registerApiComparison(
    "japicmpXzSupport",
    baselineArtifacts("xz-support"),
    tasks.named<Jar>("xzSupportJar"),
    xzSupport.compileClasspath
)

val checkApiCompatibility = tasks.register<CheckApiCompatibilityTask>("checkApiCompatibility") {
    group = "verification"
    description = "Fails when the API changes since the last release ask for a bigger version bump than the commits declare."
    baselineVersion = apiBaselineVersion
    declaredBump = semver.declaredBump
    reports.from(japicmpMain.flatMap { it.xmlOutputFile }, japicmpXzSupport.flatMap { it.xmlOutputFile })
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
            ignoreSourceSet("xzSupport")
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial"))
    options.encoding = "UTF-8"
}

// Consumers on the module path derive the module name from the manifest; without it the file name decides, and that
// changes with the version.
fun Jar.compress4jManifest(moduleName: String, title: String) = manifest {
    attributes(
        "Automatic-Module-Name" to moduleName,
        "Implementation-Title" to title,
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to "The Compress4J Project"
    )
}

tasks.jar { compress4jManifest("io.github.compress4j", project.name) }
tasks.named<Jar>("xzSupportJar") {
    compress4jManifest("io.github.compress4j.xz", "${project.name}-xz-support")
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all")
}

tasks.testCodeCoverageReport {
    dependsOn(tasks.test, integrationTest, xzSupportTest)
    executionData(
        fileTree(layout.buildDirectory).include("jacoco/*.exec")
    )
    reports {
        xml.required = true
        html.required = true
    }
    mustRunAfter(tasks.spotlessCheck, tasks.javadoc)
}

tasks.check {
    dependsOn(
        tasks.buildHealth,
        tasks.spotlessCheck,
        checkApiCompatibility,
        integrationTest,
        tasks.testCodeCoverageReport,
        // The build logic decides what gets published to Maven Central, so its tests run with everything else
        gradle.includedBuild("${rootProject.name}-build-logic").task(":test")
    )
}

sonar {
    properties {
        property("sonar.projectKey", "compress4j_compress4j")
        property("sonar.organization", "compress4j")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "src/main/java,src/xzSupport/java,src/examples/java")
        property("sonar.tests", "src/test/java,src/xzSupportTest/java,src/integrationTest/java,src/testFixtures/java")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")
        property(
            "sonar.coverage.exclusions",
            listOf(
                "src/examples/java/**/*",
                "**/*Exception.java"
            )
        )
    }
}

// Sonar needs the aggregated coverage report and the compiled classes of every analysed source set, but not the rest of
// `check` — CI already ran that in the same job, so depending on it here would only re-report the same failures.
tasks.sonar {
    dependsOn(
        tasks.testCodeCoverageReport,
        tasks.classes,
        tasks.testClasses,
        tasks.named("testFixturesClasses"),
        tasks.named("xzSupportClasses"),
        tasks.named("xzSupportTestClasses"),
        tasks.named("integrationTestClasses")
    )
}

spotless {
    ratchetFrom("origin/main")
    java {
        toggleOffOn()
        palantirJavaFormat("2.81.0").formatJavadoc(true)
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
            suppressPomMetadataWarningsFor("xzSupportApiElements")
            suppressPomMetadataWarningsFor("xzSupportJavadocElements")
            suppressPomMetadataWarningsFor("xzSupportRuntimeElements")
            suppressPomMetadataWarningsFor("xzSupportSourcesElements")
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

configure<org.jreleaser.gradle.plugin.JReleaserExtension> {
    release {
        github {
            skipTag = true // The release workflow creates and pushes the tag
            changelog {
                formatted = Active.ALWAYS
                preset = "conventional-commits"
                links = true
            }
        }
    }
    signing {
        pgp {
            active = Active.ALWAYS
            armored = true
        }
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
