# Kotlin check failed on 2026-09-24 16:54:51 UTC

```
45:> Task :app:compileReleaseKotlin FAILED
46:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1037:36 Unresolved reference 'selected'.
47:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1038:36 Unresolved reference 'selected'.
48:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1039:36 Unresolved reference 'selected'.
49:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1040:36 Unresolved reference 'selected'.
50:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1042:31 Unresolved reference 'selected'.
51:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1043:32 Unresolved reference 'selected'.
53:FAILURE: Build failed with an exception.
55:* What went wrong:
56:Execution failed for task ':app:compileReleaseKotlin'.
```

## Tail
```
 - Enhancements for Scala plugin and JUnit testing
 - Improvements for build authors and plugin developers

For more details see https://docs.gradle.org/8.13/release-notes.html

To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.13/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon JVM discovery is an incubating feature.
Daemon will be stopped at the end of the build 

> Configure project :app
Checking the license for package NDK (Side by side) 26.3.11579264 in /usr/local/lib/android/sdk/licenses
License for package NDK (Side by side) 26.3.11579264 accepted.
Preparing "Install NDK (Side by side) 26.3.11579264 v.26.3.11579264".
"Install NDK (Side by side) 26.3.11579264 v.26.3.11579264" ready.
Installing NDK (Side by side) 26.3.11579264 in /usr/local/lib/android/sdk/ndk/26.3.11579264
"Install NDK (Side by side) 26.3.11579264 v.26.3.11579264" complete.
"Install NDK (Side by side) 26.3.11579264 v.26.3.11579264" finished.

> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:preBuild
> Task :app:preReleaseBuild
> Task :app:checkReleaseAarMetadata
> Task :app:processReleaseNavigationResources
> Task :app:generateReleaseResValues
> Task :app:compileReleaseNavigationResources
> Task :app:mapReleaseSourceSetPaths
> Task :app:generateReleaseResources
> Task :app:packageReleaseResources
> Task :app:createReleaseCompatibleScreenManifests
> Task :app:extractDeepLinksRelease
> Task :app:parseReleaseLocalResources
> Task :app:processReleaseMainManifest
> Task :app:processReleaseManifest
> Task :app:processReleaseManifestForPackage
> Task :app:mergeReleaseResources
> Task :app:processReleaseResources

> Task :app:compileReleaseKotlin FAILED
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1037:36 Unresolved reference 'selected'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1038:36 Unresolved reference 'selected'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1039:36 Unresolved reference 'selected'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1040:36 Unresolved reference 'selected'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1042:31 Unresolved reference 'selected'.
e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1043:32 Unresolved reference 'selected'.

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileReleaseKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 1m 48s
16 actionable tasks: 16 executed
```
