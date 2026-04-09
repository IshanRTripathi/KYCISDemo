// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.12.3" apply false
    id("com.android.library") version "8.12.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

/**
 * Composite `includeBuild` of `../KYCIS/android-sdk` can leave `parse*LocalResources` marked
 * UP-TO-DATE after `:android-sdk:sdk:clean` while `R-def.txt` was deleted, which then breaks
 * `:android-sdk:sdk:generateDebugRFile`. Run this task (or the equivalent one-liner) when that happens.
 */
tasks.register<Exec>("fixIncludedSdkRDef") {
    group = "build"
    description =
        "Regenerates SDK R-def.txt (parseDebugLocalResources --rerun-tasks). Use if generateDebugRFile fails."
    workingDir = rootDir
    val isWindows =
        System.getProperty("os.name").orEmpty().lowercase().contains("windows")
    if (isWindows) {
        commandLine(
            "cmd",
            "/c",
            "gradlew.bat",
            ":android-sdk:sdk:parseDebugLocalResources",
            "--rerun-tasks",
            "--no-daemon",
        )
    } else {
        commandLine(
            "./gradlew",
            ":android-sdk:sdk:parseDebugLocalResources",
            "--rerun-tasks",
            "--no-daemon",
        )
    }
}