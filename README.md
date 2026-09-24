# Kourosh-AE VPN for Android

![Build](https://img.shields.io/badge/build-GitHub%20Actions-E7B84B?style=for-the-badge&logo=github&logoColor=080808)
![Android](https://img.shields.io/badge/Android-8.0%2B-080808?style=for-the-badge&logo=android&logoColor=E7B84B)
![License](https://img.shields.io/badge/license-AGPL--3.0-FF5C5C?style=for-the-badge)

**Kourosh-AE** یک کلاینت VPN بومی اندروید با رابطی سریع و هویت بصری مشکی، طلایی و قرمز است. برنامه با `VpnService` اندروید، ترافیک دستگاه را مدیریت می‌کند و چند مسیر انتقال را در یک کنسول واحد در اختیار کاربر می‌گذارد.

## امکانات

- اتصال یک‌ضربه‌ای با وضعیت زنده، زمان نشست و مصرف داده
- MASQUE روی HTTP/3، WireGuard، WARP-on-WARP، Psiphon و Tor
- پشتیبانی از TCP، UDP و QUIC در سطح دستگاه
- انتخاب کشور خروج، حالت‌های زنجیره‌ای و تونل تفکیکی برای اپ‌ها
- Quick Settings tile برای اتصال و قطع سریع
- رابط بازطراحی‌شده Kourosh-AE با تم تاریک و روشن
- ساخت خودکار APK با GitHub Actions

## دریافت APK

به بخش **Actions → Kourosh-AE Android Build → Artifacts** بروید و artifact با نام `Kourosh-AE-debug-apks` را دانلود کنید. این workflow یک APK Debug قابل نصب برای معماری‌های arm64، armv7 و x86_64 می‌سازد و به کلید خصوصی نیاز ندارد.

### درباره‌ی هشدار «برنامه امن نیست» هنگام نصب

اندروید برای هر APKی که از بیرون پلی‌استور نصب می‌شود و سازنده‌اش را تأیید نکرده باشد، هشدار **Play Protect** نشان می‌دهد؛ این هشدار درباره‌ی *منبع توزیع* است، نه درباره‌ی خود فایل. از سال ۲۰۲۶ گوگل تأیید هویت سازنده را برای نصب APK روی دستگاه‌های دارای Play Protect الزامی کرده است، پس این پیام روی هر نسخه‌ای که از گیت‌هاب دانلود کنید دیده می‌شود و از داخل برنامه قابل خاموش‌کردن نیست.

انتشارهای این پروژه با **کلید امضای خود پروژه** امضا شده‌اند. برای اطمینان از سالم‌بودن فایل دانلودشده:

1. در صفحه‌ی Release، مقدار `SHA-256` هر APK نوشته شده است؛ آن را با `sha256sum` روی فایل دانلودشده مقایسه کنید.
2. در دیالوگ Play Protect گزینه‌ی **Install anyway / نصب به‌هرحال** را بزنید.
3. اگر می‌خواهید بررسی خودکار Play Protect کل فایل را اسکن کند، پیش از نصب در Play Store گزینه‌ی **Scan apps with Play Protect** را موقتاً خاموش کنید.

## ساخت محلی

پیش‌نیازها: JDK 17، Android SDK 36، NDK `26.3.11579264`، CMake `3.22.1` و Rust stable.

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
./gradlew assembleDebug
```

خروجی در `app/build/outputs/apk/debug/` ساخته می‌شود.

## ساختار پروژه

| بخش | مسئولیت |
|---|---|
| `app/src/main/java/` | رابط Kourosh-AE و چرخه حیات VPN |
| `app/src/main/res/` | تم، لوگو و منابع رابط |
| `app/src/main/cpp/` | پل JNI و tun2socks |
| `core/aether/` | هسته شبکه Rust |
| `.github/workflows/` | ساخت خودکار APK |

## حریم خصوصی و امنیت

Kourosh-AE برای مدیریت اتصال VPN روی دستگاه طراحی شده است. کد و سیاست‌های مسیریابی را بررسی کنید و فقط نسخه‌هایی را نصب کنید که از منبع مورد اعتماد دریافت شده‌اند.

## کانال رسمی

اخبار و نسخه‌های جدید: [@timazadi](https://t.me/timazadi)

## مجوز

این پروژه تحت مجوز AGPL-3.0 منتشر می‌شود. متن کامل در [LICENSE](LICENSE) قرار دارد.
