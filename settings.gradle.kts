rootProject.name = "compress4j"

plugins {
    id("com.gradle.develocity") version("3.19")
    id("com.autonomousapps.build-health") version "2.16.0"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}
