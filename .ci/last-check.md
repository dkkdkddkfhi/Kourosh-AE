# Kotlin check failed on 2026-09-24 16:52:10 UTC

```
28:FAILURE: Build failed with an exception.
30:* What went wrong:
```

## Tail
```
Downloading https://services.gradle.org/distributions/gradle-8.13-bin.zip
.............10%.............20%.............30%.............40%.............50%.............60%.............70%.............80%.............90%.............100%

Welcome to Gradle 8.13!

Here are the highlights of this release:
 - Daemon JVM auto-provisioning
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

[Incubating] Problems report is available at: file:///home/runner/work/Kourosh-AE/Kourosh-AE/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Task 'buildRustCoreX8664' not found in root project 'KOUROSH-AE' and its subprojects. Some candidates are: 'buildRustCoreX86_64'.

* Try:
> Run gradlew tasks to get a list of available tasks.
> For more on name expansion, please refer to https://docs.gradle.org/8.13/userguide/command_line_interface.html#sec:name_abbreviation in the Gradle documentation.
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 1m 23s
```
