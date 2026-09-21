# ماتریس قابلیت‌های Kourosh-AE

## نتیجه

ممیزی شاخهٔ فعلی نشان داد بیشتر قابلیت‌های اعلام‌شده برای نسخهٔ ۲.۰.۰ از قبل در Kourosh-AE پیاده‌سازی شده‌اند و به رابط کاربری، سرویس VPN و هستهٔ Rust متصل هستند.

## وضعیت قابلیت‌ها

| قابلیت | وضعیت | محل اصلی |
|---|---|---|
| Endpoint دستی و کش Gateway | پیاده‌سازی‌شده | `MainActivity`, `CoreConfig` |
| DNS معمولی UDP، DoT و DoH | پیاده‌سازی‌شده | صفحهٔ DNS، `CoreConfig`، `smart_dns.rs` |
| پروفایل‌های مستقل و مهاجرت تنظیمات | پیاده‌سازی‌شده | `Profiles.kt`, `SettingsBackup.kt` |
| Bypass Iran | پیاده‌سازی‌شده | `MainActivity`, `kourosh_aeVpnService.kt` |
| Smart Split، TLS fragmentation و Mixed-case SNI | پیاده‌سازی‌شده | `SmartSplit.kt`, `ShardConfigs.kt`, هستهٔ Rust |
| health check و انتخاب مسیر | پیاده‌سازی‌شده | `ShardHealth.kt`, `ShardManager.kt`, Aether |
| MASQUE و MASQUE over MASQUE | پیاده‌سازی‌شده | `CoreConfig.kt`, `MimCard.kt` |
| WireGuard و WARP over WARP | پیاده‌سازی‌شده | `wireguard.rs`, `CoreConfig.kt` |
| Psiphon و انتخاب کشور | پیاده‌سازی‌شده | `PsiphonRegions.kt`, Home و Settings |
| Tor و انتخاب کشور | پیاده‌سازی‌شده | `TorManager.kt`, `TorRegions.kt` |
| SHARD و AnyTLS | پیاده‌سازی‌شده | `ShardManager.kt`, `AnyTlsManager.kt` |
| CDN edge rotation و IP سفارشی | پیاده‌سازی‌شده | `ShardEdges.kt`, `ShardConfigs.kt` |
| رابط مشکی، اتصال افقی، پینگ، سرعت و درصد اتصال | پیاده‌سازی‌شده | `MainActivity.kt`, `OrbitDialView.kt` |
| صحنهٔ زندهٔ native | پیاده‌سازی‌شده | `KouroshSceneView.kt` |

## تغییر این مرحله

در `KouroshSceneView` ساختن `Paint` و `Path` در هر فریم حذف شد. این اشیا اکنون یک‌بار ساخته می‌شوند و در هر فریم با `rewind` و به‌روزرسانی propertyها استفاده می‌شوند. انیمیشن هنگام جداشدن View از پنجره متوقف و callback آن حذف می‌شود.

## اعتبارسنجی

بیلد Debug در محیط فعلی به کامپایل نرسید، چون Android SDK نصب نیست و `ANDROID_HOME` یا `local.properties` معتبر وجود ندارد. تست Rust نیز به دلیل نصب‌نبودن `cargo` قابل اجرا نبود. بیلد نهایی باید در GitHub Actions انجام شود؛ تا موفقیت آن workflow، انتشار نسخهٔ جدید قطعی اعلام نمی‌شود.

## منابع

[1]: app/src/main/java/com/kourosh/ae/KouroshSceneView.kt "صحنهٔ زنده و اصلاح performance"
[2]: app/src/main/java/com/kourosh/ae/MainActivity.kt "رابط کاربری و تنظیمات"
[3]: app/src/main/java/com/kourosh/ae/CoreConfig.kt "پیکربندی هسته و DNS"
[4]: app/src/main/java/com/kourosh/ae/Profiles.kt "پروفایل‌ها و مهاجرت تنظیمات"
[5]: app/src/main/java/com/kourosh/ae/ShardEdges.kt "مسیرهای CDN"
[6]: core/aether/src/smart_dns.rs "موتور DNS رمزنگاری‌شده"
[7]: core/aether/src/main.rs "گزینه‌های هستهٔ Rust"
[8]: .github/workflows/build.yml "فرآیند ساخت و انتشار"
[9]: https://developer.android.com/studio/publish/upload-bundle "راهنمای رسمی انتشار AAB"
[10]: https://support.google.com/googleplay/android-developer/answer/9859152 "راهنمای رسمی Google Play"
