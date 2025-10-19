rootProject.name = "compress4j"

plugins {
    id("com.gradle.develocity") version("4.2.2")
    id("com.autonomousapps.build-health") version "2.19.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}
