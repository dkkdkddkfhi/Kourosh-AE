# Aether v2.1.0 Android-compatible backport

این شاخه به‌دلیل تفاوت ABI بین هستهٔ Android موجود و FFI نسخهٔ upstream، باینری upstream را مستقیم جایگزین نمی‌کند. جایگزینی مستقیم، نمادهای JNI فعلی (`aether_start_json` و دوستان آن) را حذف می‌کرد و اتصال VPN را می‌شکست.

در عوض، قابلیت‌های پیکربندی معرفی‌شده در Aether v2.1.0 به ABI فعلی اضافه شده‌اند:

| قابلیت | کلید payload Android | پیش‌فرض |
|---|---|---|
| سیاست کشور خروج | `exit_loc` | خاموش |
| فاصلهٔ بررسی کشور خروج | `exit_loc_secs` | ۶۰ ثانیه |
| آمار مصرف و uptime | `stats` | خاموش |
| منطقهٔ Psiphon | `psiphon_region` | خودکار |
| حالت Psiphon | `psiphon_mode` | خودکار |
| HTTP proxy برای Psiphon | `psiphon_http` | خاموش |
| فایل Bridge تور | `tor_bridge_file` | خاموش |
| منبع relayهای تور | `tor_relays` | خاموش |

`exit_loc` از دو شکل پشتیبانی می‌کند: فهرست مجاز مانند `DE,SE` و فهرست غیرمجاز مانند `!IR,AZ,RU`. parser کدها را uppercase می‌کند، ورودی‌های ناقص را نادیده می‌گیرد و در حالت خالی سیاستی اعمال نمی‌کند.

این گزینه‌ها در SharedPreferences با کلیدهای زیر نگهداری می‌شوند و فقط پس از فعال‌سازی کاربر وارد payload اتصال می‌گردند:

```text
aether_exit_loc
aether_exit_loc_secs
aether_stats
psiphon_region
psiphon_mode
psiphon_http
tor_bridge_file
tor_relays
```

متغیرهای runtime متناظر با نام‌گذاری upstream نیز تنظیم می‌شوند: `AETHER_EXIT_LOC`، `AETHER_EXIT_LOC_SECS`، `AETHER_STATS`، `AETHER_PSIPHON_REGION`، `AETHER_PSIPHON_MODE`، `AETHER_PSIPHON_HTTP`، `AETHER_TOR_BRIDGE_FILE` و `AETHER_TOR_RELAYS`.

## محدودیت فعلی

این backport، قرارداد تنظیمات و policy parser را بدون شکستن JNI اضافه می‌کند. موتور Android فعلی هنوز برای تغییر edge در زمان اجرا به یک callback صریح از لایهٔ geolocation و چرخهٔ reconnect نیاز دارد؛ بنابراین `exit_loc` تا زمانی که UI آن را فعال نکند و country lookup موفق نباشد، خاموش باقی می‌ماند. باینری upstream و ماژول‌های CLI-only آن مستقیماً داخل APK کپی نشده‌اند.

## تست

تست‌های واحد Rust برای allow-list، deny-list، ورودی‌های ناقص و حالت خاموش در `core/aether/src/exitloc.rs` قرار دارند.
