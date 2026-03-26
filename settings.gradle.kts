pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
