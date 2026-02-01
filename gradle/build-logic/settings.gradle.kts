// https://github.com/gradle/gradle/issues/30299
rootProject.name = "plugins"

dependencyResolutionManagement { versionCatalogs { create("libs") { from(files("../libs.versions.toml")) } } }
