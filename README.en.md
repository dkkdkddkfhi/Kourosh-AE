# Kourosh-AE VPN for Android

![Build](https://img.shields.io/badge/build-GitHub%20Actions-E7B84B?style=for-the-badge&logo=github&logoColor=080808)
![Android](https://img.shields.io/badge/Android-8.0%2B-080808?style=for-the-badge&logo=android&logoColor=E7B84B)
![License](https://img.shields.io/badge/license-AGPL--3.0-FF5C5C?style=for-the-badge)

**Kourosh-AE** is a native Android VPN client with a fast console and a black, gold, and red visual identity. It uses Android `VpnService` to manage device traffic and provides multiple transport paths in one focused interface.

## Features

- One-tap connection with live status, session time, and traffic metrics
- MASQUE over HTTP/3, WireGuard, WARP-on-WARP, Psiphon, and Tor
- Device-wide TCP, UDP, and QUIC support
- Exit-country selection, chained modes, and per-app split tunneling
- Quick Settings tile for instant connect and disconnect
- Fully refreshed Kourosh-AE dark and light themes
- Reproducible APK builds through GitHub Actions

## Download an APK

Open **Actions → Kourosh-AE Android Build → Artifacts** and download `Kourosh-AE-debug-apks`. The workflow builds installable Debug APKs for arm64, armv7, and x86_64 without requiring a private signing key.

### About the "app is not safe" warning at install time

Android shows a **Play Protect** warning for every APK installed from outside Google Play when the publisher has not been verified. The warning is about *who distributes the file*, not about the file itself. From 2026 Google requires developer verification for APKs on certified devices, so every release downloaded from GitHub shows it, and nothing inside the APK can turn it off.

Releases here are signed with this project's own release key. To be sure of a download:

1. Each release lists the `SHA-256` of every APK; compare it with `sha256sum` on the file you downloaded.
2. Choose **Install anyway** in the Play Protect dialog.
3. To have Play Protect scan the file instead, turn **Scan apps with Play Protect** off in the Play Store settings before installing.

## Build locally

Requirements: JDK 17, Android SDK 36, NDK `26.3.11579264`, CMake `3.22.1`, and stable Rust.

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
./gradlew assembleDebug
```

The APKs are written to `app/build/outputs/apk/debug/`.

## Project structure

| Area | Responsibility |
|---|---|
| `app/src/main/java/` | Kourosh-AE UI and Android VPN lifecycle |
| `app/src/main/res/` | Theme, logo, and UI resources |
| `app/src/main/cpp/` | JNI bridge and tun2socks |
| `core/aether/` | Rust networking core |
| `.github/workflows/` | Automated APK builds |

## Privacy and security

Kourosh-AE is designed to manage a device VPN connection. Review the source and routing policy before use, and install builds only from sources you trust.

## Official channel

News and releases: [@timazadi](https://t.me/timazadi)

## License

This project is distributed under AGPL-3.0. See [LICENSE](LICENSE).
