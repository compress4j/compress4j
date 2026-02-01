import org.gradle.api.internal.FeaturePreviews

rootProject.name = "compress4j"

pluginManagement { includeBuild("gradle/build-logic") { name = rootProject.name + "-build-logic" } }

plugins {
    id("build-health")
    id("gradle-develocity")
    id("foojay-resolver-convention")
}

enableFeaturePreview(FeaturePreviews.Feature.TYPESAFE_PROJECT_ACCESSORS.name)

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}
