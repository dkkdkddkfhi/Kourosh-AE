# Gradle failure on 2026-09-24 17:05:22 UTC

## Errors
```
4870:> Task :app:compileReleaseKotlin FAILED
4871:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1037:36 Unresolved reference 'selected'.
4872:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1038:36 Unresolved reference 'selected'.
4873:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1039:36 Unresolved reference 'selected'.
4874:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1040:36 Unresolved reference 'selected'.
4875:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1042:31 Unresolved reference 'selected'.
4876:e: file:///home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/java/com/kourosh/ae/AuroraWidgets.kt:1043:32 Unresolved reference 'selected'.
4878:FAILURE: Build failed with an exception.
4880:* What went wrong:
4881:Execution failed for task ':app:compileReleaseKotlin'.
```

## Tail
```
C/C++:         if (n1->link[1] = n2->link[1]) {
C/C++:             ~~~~~~~~~~~~^~~~~~~~~~~~~
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/structure/BAVL.h:398:25: note: place parentheses around the assignment to silence this warning
C/C++:         if (n1->link[1] = n2->link[1]) {
C/C++:                         ^
C/C++:             (                        )
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/structure/BAVL.h:398:25: note: use '==' to turn this assignment into an equality comparison
C/C++:         if (n1->link[1] = n2->link[1]) {
C/C++:                         ^
C/C++:                         ==
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/structure/BAVL.h:401:25: warning: using the result of an assignment as a condition without parentheses [-Wparentheses]
C/C++:         if (n2->link[1] = temp) {
C/C++:             ~~~~~~~~~~~~^~~~~~
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/structure/BAVL.h:401:25: note: place parentheses around the assignment to silence this warning
C/C++:         if (n2->link[1] = temp) {
C/C++:                         ^
C/C++:             (                 )
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/structure/BAVL.h:401:25: note: use '==' to turn this assignment into an equality comparison
C/C++:         if (n2->link[1] = temp) {
C/C++:                         ^
C/C++:                         ==
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/tun2socks/tun2socks.c:671:17: warning: using the result of an assignment as a condition without parentheses [-Wparentheses]
C/C++:     while (node = LinkedList1_GetFirst(&tcp_clients)) {
C/C++:            ~~~~~^~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/tun2socks/tun2socks.c:671:17: note: place parentheses around the assignment to silence this warning
C/C++:     while (node = LinkedList1_GetFirst(&tcp_clients)) {
C/C++:                 ^
C/C++:            (                                        )
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/tun2socks/tun2socks.c:671:17: note: use '==' to turn this assignment into an equality comparison
C/C++:     while (node = LinkedList1_GetFirst(&tcp_clients)) {
C/C++:                 ^
C/C++:                 ==
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/tun2socks/tun2socks.c:1517:20: warning: using the result of an assignment as a condition without parentheses [-Wparentheses]
C/C++:         } while (p = p->next);
C/C++:                  ~~^~~~~~~~~
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/tun2socks/tun2socks.c:1517:20: note: place parentheses around the assignment to silence this warning
C/C++:         } while (p = p->next);
C/C++:                    ^
C/C++:                  (          )
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/tun2socks/tun2socks.c:1517:20: note: use '==' to turn this assignment into an equality comparison
C/C++:         } while (p = p->next);
C/C++:                    ^
C/C++:                    ==
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/tun2socks/tun2socks.c:1843:88: warning: expression result unused [-Wunused-value]
C/C++:     ASSERT_EXECUTE(pbuf_copy_partial(p, client->buf + client->buf_used, p->tot_len, 0) == p->tot_len)
C/C++:                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ^  ~~~~~~~~~~
C/C++: /home/runner/work/Kourosh-AE/Kourosh-AE/app/src/main/cpp/badvpn/misc/debug.h:116:34: note: expanded from macro 'ASSERT_EXECUTE'
C/C++:     #define ASSERT_EXECUTE(e) { (e); }
C/C++:                                  ^
C/C++: 13 warnings generated.

> Task :app:mergeReleaseJniLibFolders
> Task :app:checkReleaseDuplicateClasses
> Task :app:buildKotlinToolingMetadata
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:mergeReleaseNativeLibs
> Task :app:checkReleaseAarMetadata
> Task :app:processReleaseNavigationResources
> Task :app:stripReleaseDebugSymbols
> Task :app:compileReleaseNavigationResources
> Task :app:generateReleaseResValues
> Task :app:mapReleaseSourceSetPaths
> Task :app:generateReleaseResources
> Task :app:extractReleaseNativeSymbolTables
> Task :app:packageReleaseResources
> Task :app:createReleaseCompatibleScreenManifests
> Task :app:extractDeepLinksRelease
> Task :app:parseReleaseLocalResources
> Task :app:processReleaseMainManifest
> Task :app:processReleaseManifest
> Task :app:processReleaseManifestForPackage
> Task :app:javaPreCompileRelease
> Task :app:mergeReleaseStartupProfile
> Task :app:mergeReleaseArtProfile
> Task :app:mergeReleaseShaders
> Task :app:compileReleaseShaders NO-SOURCE
> Task :app:generateReleaseAssets UP-TO-DATE
> Task :app:mergeReleaseAssets
> Task :app:compressReleaseAssets
> Task :app:extractReleaseVersionControlInfo
> Task :app:extractProguardFiles
> Task :app:collectReleaseDependencies
> Task :app:sdkReleaseDependencyData
> Task :app:validateSigningRelease
> Task :app:writeReleaseAppMetadata
> Task :app:writeReleaseSigningConfigVersions
> Task :app:processApplicationManifestReleaseForBundle
> Task :app:configureReleaseDependencies
> Task :app:parseReleaseIntegrityConfig
> Task :app:mergeReleaseResources
> Task :app:desugarReleaseFileDependencies
> Task :app:mergeReleaseNativeDebugMetadata
> Task :app:processReleaseResources
> Task :app:mergeExtDexRelease
> Task :app:optimizeReleaseResources
> Task :app:bundleReleaseResources

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

BUILD FAILED in 14m 34s
52 actionable tasks: 52 executed
```
