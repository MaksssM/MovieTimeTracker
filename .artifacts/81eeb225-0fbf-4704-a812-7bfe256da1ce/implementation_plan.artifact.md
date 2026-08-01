# Implementation Plan - Fix Incompatible Gradle JVM Version

The project's Gradle version (8.11.1) is incompatible with the selected JVM version (25). Gradle 8.11.1 only supports Java up to version 23. To resolve this, we will upgrade the Gradle wrapper to version 9.6.1 and update the Android Gradle Plugin (AGP) and related plugins to ensure compatibility.

## User Review Required

> [!IMPORTANT]
> This upgrade involves major version changes for Gradle (8.x to 9.x) and Android Gradle Plugin (8.x to 9.x). While this resolves the JVM incompatibility, it may introduce some changes in build behavior.

## Proposed Changes

### [root]

#### [MODIFY] [gradle-wrapper.properties](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/gradle/wrapper/gradle-wrapper.properties)
- Upgrade `distributionUrl` to `https://services.gradle.org/distributions/gradle-9.6.1-bin.zip`.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/build.gradle.kts)
- Upgrade plugin versions:
    - `com.android.application`: `8.9.2` -> `9.3.1`
    - `org.jetbrains.kotlin.android`: `2.0.21` -> `2.4.10`
    - `com.google.dagger.hilt.android`: `2.48` -> `2.60.1`
    - `com.google.devtools.ksp`: `2.0.21-1.0.28` -> `2.4.10-1.0.30` (or compatible)
    - `com.google.gms.google-services`: `4.4.2` -> `4.5.0`

#### [MODIFY] [libs.versions.toml](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/gradle/libs.versions.toml)
- Update `agp` and `kotlin` versions to match the root build file.

### [app]

#### [MODIFY] [build.gradle.kts](file:///C:/Users/maksa/Desktop/Projects/MovieTimeTracker/app/build.gradle.kts)
- Update `kotlin-serialization` plugin version if needed (currently `1.9.0`, should be updated to match Kotlin 2.4.10).

## Verification Plan

### Automated Tests
- Run `gradlew :app:assembleDebug` (via `gradle_build`) to verify the build process completes successfully.
- Run `gradlew :app:testDebugUnitTest` to ensure tests still pass.

### Manual Verification
- Perform a Gradle Sync in the IDE to ensure the "Incompatible Gradle JVM version" error is gone.
