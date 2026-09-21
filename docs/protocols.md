# متدهای اتصال

این سند مسیر واقعی اجرای دو متد **MASQUE** و **SHARD** را در Kourosh-AE توضیح می‌دهد. مقصود از «متد» در این پروژه فقط نام انتخاب‌شده در رابط کاربری نیست؛ هر متد یک مسیر کامل از تنظیمات Kotlin تا سرویس VPN، پل JNI و هستهٔ Rust دارد.

## ۱. مسیر مشترک اتصال

`MainActivity` پروتکل، تنظیمات کاربر و گزینه‌های routing را می‌خواند و آن‌ها را به `KouroshAeVpnService` می‌دهد. سرویس پس از ساخت رابط TUN، تنظیمات را از `CoreConfig` به JSON تبدیل می‌کند. `NativeCore` این تنظیمات و descriptor رابط TUN را از طریق JNI به `libaether.so` منتقل می‌کند. هستهٔ Rust پس از آماده‌شدن تونل، وضعیت آماده‌بودن را به سرویس برمی‌گرداند؛ رابط کاربری فقط در این مرحله حالت **Connected** را نشان می‌دهد. این مرز از نمایش اتصال کاذب هنگام کامل‌نشدن handshake جلوگیری می‌کند.[1] [2] [3]

## ۲. MASQUE

### ۲.۱ نقش متد

MASQUE مسیر اصلی مبتنی بر HTTP/3 و QUIC است. در صورت نیاز، هسته می‌تواند به مسیر HTTP/2 روی TCP/443 با TLS fragmentation برگردد. SNI پیش‌فرض، pinهای TLS، حالت transport و گزینه‌های fragmentation در هستهٔ Rust تعریف شده‌اند؛ لایهٔ Kotlin فقط انتخاب و مقداردهی آن‌ها را انجام می‌دهد.[4] [5]

### ۲.۲ ساخت پیکربندی

`CoreConfig.json()` این مقادیر را تولید می‌کند:

- `protocol`: مقدار `masque` برای اتصال معمولی؛
- `masque_transport`: مقدار انتخاب‌شده، پیش‌فرض `h3`؛
- مسیر cache مربوط به gateway discovery؛
- `forced_peer` در صورت واردکردن endpoint دستی؛
- وضعیت `h2_fragmentation`، پروفایل obfuscation و `mixed_case_sni`؛
- DNSهای UDP، DoT و DoH و routeهای direct/block.

Endpoint دستی فقط یک peer را pin می‌کند. این مقدار به ladder متدهای دیگر منتقل نمی‌شود، زیرا یک gateway MASQUE را نمی‌توان به‌عنوان peer WireGuard یا WARP استفاده کرد.[2]

### ۲.۳ کش و discovery

هسته در صورت فعال‌بودن discovery از cache gateway استفاده می‌کند. این cache در مسیر فایل برنامه نگهداری می‌شود و برای اتصال بعدی last-known-good endpoint را در اختیار dialer قرار می‌دهد. نتیجه، کاهش زمان اتصال‌های تکراری است؛ بااین‌حال cache جایگزین health check نیست و endpoint ناموفق باید توسط probing کنار گذاشته شود.[2] [5]

### ۲.۴ MASQUE over MASQUE

گزینهٔ `mim_armed`، در صورت فعال‌شدن کاربر، مقدار پروتکل را از `masque` به `mim` تبدیل می‌کند. این بازنویسی داخل `CoreConfig` انجام می‌شود تا اتصال از Home، Quick Settings و reconnect رفتار یکسانی داشته باشد. این حالت به‌صورت پیش‌فرض خاموش است، زیرا دو handshake و دو لایهٔ transport دارد و معمولاً از MASQUE تک‌لایه کندتر است.[2]

### ۲.۵ زنجیرهٔ Psiphon و Tor روی WARP

برای زنجیره‌های داخلی، MASQUE یا transportهای جایگزین به‌عنوان **outer leg** اجرا می‌شوند و روی پورت loopback جداگانه سرویس می‌دهند. `SOCKS_PORT` برای مسیر اصلی و `CHAIN_SOCKS_PORT` برای outer leg دو مقدار مستقل دارند. این جداسازی از bind شدن دو سرویس روی یک پورت و از انتشار outer leg روی شبکهٔ محلی جلوگیری می‌کند.[2] [3]

نردبان پیش‌فرض outer transport عبارت است از `masque`، سپس `wireguard` و در پایان `gool`، مگر اینکه کاربر transport مشخصی را pin کرده باشد. transport موفق به‌عنوان سابقهٔ دستگاه ذخیره می‌شود تا اتصال بعدی از گزینهٔ مناسب‌تری آغاز شود.[2]

## ۳. SHARD

### ۳.۱ مدل داده

SHARD از یک subscription متنی تغذیه می‌شود. هر خط معتبر به یک `ShardNode` تبدیل می‌شود. schemeهای قابل‌اجرا `vless`، `trojan` و `anytls` هستند. برای VLESS و Trojan، اطلاعات credential، address، port، network، security، WebSocket path، Host، SNI، fingerprint، cipher suites، ALPN و final mask حفظ می‌شود. برنامه مقادیر حساس transport را عادی‌سازی یا حدس نمی‌زند؛ ورودی نامعتبر حذف می‌شود.[6]

هویت پایدار node از مشخصات اتصال ساخته می‌شود و label تبلیغاتی subscription در آن دخالت ندارد. بنابراین تغییر label از طرف publisher، سابقهٔ latency و health node را بی‌دلیل از بین نمی‌برد.[6]

### ۳.۲ parsing و ساخت outbound

`ShardConfigs.parse()` خطوط خالی و metadata را کنار می‌گذارد، schemeهای ناشناخته را رد می‌کند و nodeهای تکراری را deduplicate می‌کند. سپس `tunnelConfig()` پیکربندی engine مناسب را می‌سازد:

- `vless` و `trojan` به Xray outbound تبدیل می‌شوند؛
- `anytls` به sidecar اختصاصی AnyTLS می‌رود؛
- پارامترهای TLS شامل server name، fingerprint، cipher suites و ALPN حفظ می‌شوند؛
- final mask به‌صورت JSON معتبر وارد stream settings می‌شود؛
- برای VLESS multiplexing با concurrency محدود فعال می‌شود.

یک node خراب نباید کل subscription را از کار بیندازد؛ خطا در یک خط فقط همان candidate را حذف می‌کند.[6]

### ۳.۳ CDN edge fan-out

بسیاری از nodeها پشت Cloudflare قرار دارند. آدرس موجود در URI لزوماً origin نیست؛ Host و SNI مسیر مقصد را مشخص می‌کنند. `ShardEdges.expand()` برای nodeهایی که Host دارند، روی پورت‌های مورد پشتیبانی Cloudflare هستند و address آن‌ها داخل rangeهای معتبر Cloudflare قرار دارد، چند variant با edgeهای مختلف می‌سازد.[7]

این fan-out کنترل‌شده است و همهٔ IPهای Cloudflare را اسکن نمی‌کند. address خود subscription همیشه حفظ می‌شود. variantها با offset وابسته به index node تولید می‌شوند تا در اولین race همهٔ nodeها روی یک edge واحد شروع نکنند. پس از ایجاد سابقه، ranking سلامت جای ترتیب اولیه را می‌گیرد.[7]

اگر کاربر Cloudflare IP سفارشی وارد کرده باشد، fan-out غیرفعال می‌شود و برای هر node فقط یک variant باقی می‌ماند. این کار از race کردن چند candidate یکسان جلوگیری می‌کند.[6] [7]

### ۳.۴ health ranking و race

`ShardManager` ابتدا subscription را parse می‌کند، nodeها را expand می‌کند و candidateهای قابل اجرا را برای probing یا شروع تونل در اختیار می‌گیرد. `ShardHealth` latency، موفقیت و شکست‌های متوالی را به‌صورت per-node و per-edge نگهداری می‌کند. candidateهای بهتر زودتر امتحان می‌شوند و candidateهای شکست‌خورده demote می‌شوند. این حافظه روی هر شبکه جداگانه کامل نیست، اما برای جلوگیری از تکرار مسیرهای کند در اتصال‌های بعدی استفاده می‌شود.[7] [8]

در صورت لغو اتصال، race باید متوقف شود و قبل از launch نهایی یک checkpoint لغو وجود دارد. این ترتیب مانع باقی‌ماندن process آزمایشی بعد از قطع سریع یا double-tap می‌شود.[8]

### ۳.۵ Smart Split

در صورت فعال‌بودن Smart Split، SHARD قبل از fallback به node-only، پروفایل‌های fragmentation را به‌ترتیب subscription امتحان می‌کند. هر profile budget probing جداگانه دارد. اولین profile که بتواند SNI یا مقصد مسدودشده را عبور دهد، برای tunnel استفاده می‌شود. profile موفق cache می‌شود تا اتصال‌های بعدی probing کامل را تکرار نکنند. اگر هیچ profile مناسب نباشد، مسیر به پیکربندی node-only برمی‌گردد.[8] [9]

### ۳.۶ refresh و سیاست remote

لیست nodeها و edgeها دادهٔ runtime هستند. `ShardSubscription` و `RemotePolicy` آن‌ها را با فاصلهٔ زمانی محدود refresh می‌کنند، از ETag استفاده می‌کنند و ورودی‌ها را قبل از استفاده validate می‌کنند. اعتبارسنجی edge شامل dotted-quad بودن، قرارگرفتن در range معتبر Cloudflare و محدودیت تعداد ورودی‌ها است. برنامه نباید برای refresh منتظر برقراری تونل بماند، چون خود دادهٔ refresh برای برقراری تونل لازم است.[10] [11]

## ۴. تفاوت عملی MASQUE و SHARD

| موضوع | MASQUE | SHARD |
|---|---|---|
| منبع endpoint | gateway discovery یا endpoint دستی | subscription و remote edge policy |
| engine اصلی | هستهٔ Aether در Rust | Xray و AnyTLS sidecar، با health manager |
| مسیر جایگزین | HTTP/2 و TLS fragmentation، سپس outer ladder در chain | edge variant، node دیگر و Smart Split profile |
| حافظهٔ موفقیت | gateway cache و last-known-good transport | health ranking برای node و edge |
| ورودی حساس | SNI، pin، transport و obfuscation | credential، Host، SNI، final mask و fingerprint |
| استفادهٔ مناسب | تونل عمومی دستگاه با مسیر Cloudflare/WARP | pool چندمسیره با امکان rotation و fallback |

## ۵. خطاهای رایج در توسعه

تغییر دادن `finalMask`، cipher suites یا fingerprint بدون تغییر هماهنگ بقیهٔ پارامترها می‌تواند ClientHello را از شکل مورد انتظار subscription خارج کند. همچنین نباید edge fan-out روی node بدون Host یا روی آدرس خارج از range Cloudflare انجام شود. در MASQUE نیز نباید endpoint دستی را به outer ladder chain منتقل کرد. این محدودیت‌ها عمداً در لایهٔ ساخت config اعمال شده‌اند تا خطا پیش از شروع process آشکار شود.[2] [6] [7]

## ۶. تست و اعتبارسنجی

تست‌های parser، validation و ساخت config باید بدون شبکه قابل اجرا باشند. تست اتصال واقعی باید در GitHub Actions یا دستگاه Android دارای SDK، NDK، Rust targets و دسترسی شبکه انجام شود. محیط sandbox فعلی برای این پروژه Android SDK و `cargo` ندارد؛ بنابراین این مستند، شرح معماری و کد موجود است و ادعای موفقیت بیلد محلی یا اتصال زنده نمی‌کند.

## References

[1]: ../app/src/main/java/com/kourosh/ae/MainActivity.kt "مدیریت رابط کاربری و وضعیت اتصال"
[2]: ../app/src/main/java/com/kourosh/ae/CoreConfig.kt "ساخت پیکربندی پروتکل‌ها و chainها"
[3]: ../app/src/main/java/com/kourosh/ae/kourosh_aeVpnService.kt "چرخهٔ حیات سرویس VPN"
[4]: ../core/aether/src/consts.rs "ثابت‌های SNI، TLS pin و MASQUE"
[5]: ../core/aether/src/main.rs "ورودی‌های هستهٔ Aether"
[6]: ../app/src/main/java/com/kourosh/ae/ShardConfigs.kt "parser و سازندهٔ config متد SHARD"
[7]: ../app/src/main/java/com/kourosh/ae/ShardEdges.kt "گسترش nodeها روی edgeهای Cloudflare"
[8]: ../app/src/main/java/com/kourosh/ae/ShardManager.kt "race، probing و راه‌اندازی tunnel"
[9]: ../app/src/main/java/com/kourosh/ae/SmartSplit.kt "پروفایل‌های Smart Split و fragmentation"
[10]: ../app/src/main/java/com/kourosh/ae/RemotePolicy.kt "دریافت و اعتبارسنجی policy و edgeها"
[11]: ../app/src/main/java/com/kourosh/ae/ShardSubscription.kt "refresh subscription و cache آن"
[12]: ../app/src/main/java/com/kourosh/ae/ShardHealth.kt "حافظهٔ latency و سلامت candidateها"
[13]: ../docs/architecture.md "معماری کلی Android، JNI و Rust"
[14]: https://developer.android.com/reference/android/net/VpnService "مستندات رسمی Android VpnService"
[15]: https://docs.github.com/en/actions "مستندات رسمی GitHub Actions"
