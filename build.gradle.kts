plugins {
    `java-library`
    `maven-publish`
    jacoco
    signing
    alias(libs.plugins.spotless)
    alias(libs.plugins.sonarqube)
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jakarta.annotation.api)
    api(libs.org.apache.commons.commons.compress)
    api(libs.org.tukaani.xz)

    testImplementation(platform(libs.junit.bom))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)

    testRuntimeOnly(libs.junit.platform.launcher)

}

group = "org.compress4j"
version = "1.3.0-SNAPSHOT"
description = "A simple archiving and compression library for Java."

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

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        csv.required = false
        html.required = true
    }
}

sonar {
    properties {
        property("sonar.projectKey", "austek_compress4j")
        property("sonar.organization", "austek")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.sonar {
    dependsOn(tasks.jacocoTestReport)
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

publishing {
    repositories {
        maven {
            name = "mavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = "SONATYPE_USERNAME".byProperty
                password = "SONATYPE_PASSWORD".byProperty
            }
        }
        maven {
            name = "sonatypeSnapshot"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            credentials {
                username = "SONATYPE_USERNAME".byProperty
                password = "SONATYPE_PASSWORD".byProperty
            }
        }
    }
    publications.withType<MavenPublication> {
        artifactId = project.name
        from(components["java"])
        pom {
            name = project.name
            description = project.description
            url = "https://github.com/austek/compress4j"
            scm {
                connection = "scm:git:https://github.com/austek/compress4j.git"
                developerConnection = "scm:git:git@github.com:austek/compress4j.git"
                url = "https://github.com/austek/compress4j.git"
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

val signingKey = "SIGNING_KEY".byProperty
val signingPwd = "SIGNING_PWD".byProperty
if (signingKey.isNullOrBlank() || signingPwd.isNullOrBlank()) {
    logger.info("Signing disabled as the GPG key was not found")
} else {
    logger.info("GPG Key found - Signing enabled")
}

signing {
    useInMemoryPgpKeys(signingKey, signingPwd)
    sign(publishing.publications)
    isRequired = !(signingKey.isNullOrBlank() || signingPwd.isNullOrBlank())
}

val String.byProperty: String? get() = providers.gradleProperty(this).orNull
