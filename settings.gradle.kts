rootProject.name = "compress4j"

plugins {
    id("com.gradle.develocity") version("3.19")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}
