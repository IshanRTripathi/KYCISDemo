pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.12.3"
        id("com.android.library") version "8.12.3"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "KycDemoApp"

// Use KYCIS android-sdk as SDK source; builds from source on every run
includeBuild("../KYCIS/android-sdk") {
    dependencySubstitution {
        substitute(module("com.kycis:kycis-sdk")).using(project(":sdk"))
    }
}

include(":app")
