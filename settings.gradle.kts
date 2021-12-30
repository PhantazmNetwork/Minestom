enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
        mavenCentral()
    }
    versionCatalogs {
        create("global") {
            from(files("gradle/global.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

rootProject.name = "Minestom"
include("code-generators")
include("jmh-benchmarks")
include("demo")