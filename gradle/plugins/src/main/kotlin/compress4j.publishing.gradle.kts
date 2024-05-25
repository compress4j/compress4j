
plugins {
    `maven-publish`
    signing
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
