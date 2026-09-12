# ROJAN Customer Android — RC-1 Security Audit

**تاریخ:** 2026-09-10
**محدوده:** فقط اپلیکیشن Customer (`ai.rojan.designlab`، فلیور `customer`). Manager/Reception خارج از محدوده‌اند.
**نوع بررسی:** صرفاً Audit، فقط بررسی کد/کانفیگ (read-only) — **هیچ فایلی تغییر یا commit نشد.**
**روش:** بررسی مستقیم سورس‌کد + کانفیگ Gradle + وابستگی‌ها؛ برای بخش وابستگی‌ها از جستجوی وب برای CVEهای شناخته‌شده هم استفاده شد (نتایج در بخش 7 با منبع ذکر شده‌اند).

---

## Executive Summary

معماری امنیتی این اپ **به‌طور کلی قوی‌تر از حد معمول یک اپ اول‌نسخه است**: توکن‌ها با AES-256-GCM روی Android Keystore رمزنگاری می‌شوند (نه فقط SharedPreferences ساده)، هیچ‌جا در کل کدبیس `Log.*`/`println` وجود ندارد، release build اجباراً minify+shrink+signed است، `HttpLoggingInterceptor` در release به‌صورت قطعی خاموش است (نه فقط verbosity پایین)، و EXIF/GPS عکس‌های آواتار/کاور با re-encode کامل حذف می‌شوند. هیچ **P0** واقعی پیدا نشد.

سه مورد **P1/P2** واقعی پیدا شد که همگی قبل از انتشار عمومی (Play Store) قابل رفع سریع هستند، مهم‌ترینشان یک ناهماهنگی مستندسازی/پیاده‌سازی در backup exclusion است: قانون استثنای backup فعلی یک فایل DataStore **مرده و حذف‌شده از کد** (`role_preferences`) را محافظت می‌کند، در حالی‌که فایل DataStore **واقعاً فعال** فعلی (`auth_session_preferences`، حاوی personId کاربر لاگین‌شده) اصلاً در لیست استثناها نیست.

**تصمیم نهایی در بخش "Release Decision" در انتهای گزارش.**

---

## جدول خلاصه یافته‌ها

| # | یافته | سطح ریسک | بخش |
|---|---|---|---|
| 1 | `auth_session_preferences` (personId فعال) در Android Auto Backup مستثنی نشده؛ قانون فعلی یک فایل DataStore مرده را محافظت می‌کند | **P1** | 1، 3 |
| 2 | نبود `FLAG_SECURE` — امکان screenshot/screen-record روی صفحه OTP و صفحات دیگر، و نمایش محتوا در thumbnail لیست اپ‌های اخیر | **P2** | 5 |
| 3 | عدم تنظیم صریح `usesCleartextTraffic="false"` / عدم وجود Network Security Config (رفتار فعلی درست است ولی implicit است) | **P2** | 1، 4 |
| 4 | Coil در نسخه‌ی 2.7.0 (`io.coil-kt`) — یک major version عقب‌تر از خط فعلی Coil 3.x | **P2** | 7 |
| 5 | عدم وجود certificate pinning روی هیچ‌کدام از 3 کلاینت OkHttp | **P2 (اختیاری)** | 4 |
| 6 | عدم وجود ابزار خودکار بررسی وابستگی‌های قدیمی/آسیب‌پذیر در CI (`NewerVersionAvailable` در lint عمداً خاموش است) | **P2** | 7 |
| — | همه موارد دیگر بررسی‌شده: **بدون یافته** (نکات مثبت تأییدشده در ادامه فهرست شده‌اند) | — | همه بخش‌ها |

---

## 1. AndroidManifest.xml

**Evidence:** `app/src/main/AndroidManifest.xml`

- **Permissions:** فقط یک permission، `android.permission.INTERNET` (خط 5). هیچ permission اضافی (موقعیت مکانی، دوربین، مخاطبین، حافظه) درخواست نمی‌شود — سطح exposure بسیار پایین.
- **Exported components:** فقط `MainActivity` (خط 17-19) با `android:exported="true"`. این اجباری است چون تنها activity دارای intent-filter با `MAIN`/`LAUNCHER` است (خط 39-43) — از API 31 به بعد Android همین را الزامی می‌کند. هیچ Activity/Service/BroadcastReceiver/ContentProvider اضافه‌ای در manifest تعریف نشده — سطح حمله از طریق IPC عملاً صفر است.
- **Deep links:** هیچ intent-filter اضافه‌ای با `<data android:scheme=...>` وجود ندارد. **بدون deep link** — هیچ ورودی خارجی قابل‌سوءاستفاده از این مسیر وجود ندارد.
- **Backup configuration** — **یافته P1، جزئیات در بخش 3.**
- **Debug exposure:** `android:debuggable` در خود manifest ست نشده (این مقدار از `buildTypes` در Gradle می‌آید — بخش 2). در manifest هیچ نشانه‌ای از یک activity/تست دیباگ که در release هم export بماند دیده نشد.

**Recommended Fix:** برای موارد بدون یافته، اقدامی لازم نیست. برای backup — به بخش 3 مراجعه شود.

---

## 2. app/build.gradle.kts

**Evidence:** `app/build.gradle.kts`

| بررسی | نتیجه | خط مرجع |
|---|---|---|
| `debuggable` در release | ست نشده → پیش‌فرض AGP (`false`)، فقط `debug` buildType آن را `true` می‌کند و آن buildType هم override نشده | کل فایل، بلوک `buildTypes` (197-221) |
| `isMinifyEnabled` | `true` برای release | خط 205 |
| `isShrinkResources` | `true` برای release | خط 206 |
| Signing config | واقعی، از `keystore.properties` (gitignored) یا env var؛ v2+v3 فعال، v1 درست غیرفعال (minSdk=24 نیازی ندارد)؛ **production release بدون signing واقعی اصلاً build نمی‌شود** (fail سخت) | خط 179-195، 217-219، 275-289 |
| Production API URL | `https://api.rojanai.ir/` — commit‌شده به‌عنوان default، با یک guard در build-time که فرمت را الزاماً `https://` + trailing slash می‌کند | خط 62-77، 258-273 |
| BuildConfig values | فقط `API_BASE_URL` (per-flavor) — هیچ secret/کلید API در BuildConfig نیست | خط 151-176 |
| Hardcoded secrets در این فایل | **پیدا نشد** — همه credential از `keystore.properties`/env var خوانده می‌شوند، هیچ مقدار واقعی commit نشده | — |

**نکته:** برای `customerDevRelease`/`customerStagingRelease` (نه production)، اگر keystore محلی تنظیم نشده باشد build **بدون امضا** انجام می‌شود — این عمدی و مستند است (خط 20-22) و فقط برای build‌های داخلی/تست معنا دارد، نه برای توزیع. برای مسیر production گاردِ اجباری وجود دارد.

**Recommended Fix:** هیچ اقدامی لازم نیست — این بخش از نظر امنیتی کاملاً درست پیاده‌سازی شده.

---

## 3. Authentication & Token Security

### توکن‌ها (access/refresh) — ✅ پیاده‌سازی قوی

**Evidence:** `app/src/main/java/ai/rojan/designlab/data/local/SecureTokenStore.kt`

- توکن‌ها با **AES-256-GCM** رمزنگاری می‌شوند، کلید مستقیماً از **Android Keystore** (`AndroidKeyStore`) گرفته می‌شود و هرگز خارج از سخت‌افزار/OS-protected storage نمی‌رود (خط 30-47).
- فقط ciphertext (IV + متن رمزشده، Base64) در یک `SharedPreferences` عادی (`secure_token_preferences`, `MODE_PRIVATE`) ذخیره می‌شود (خط 68-73).
- `TokenRepositoryImpl.decryptStored` هر خطای decrypt را با `runCatching{}.getOrNull()` می‌گیرد — یعنی اگر کلید Keystore به هر دلیلی در دسترس نباشد (مثلاً بعد از restore روی دستگاه دیگر)، اپ کرش نمی‌کند و session به‌درستی نامعتبر تلقی می‌شود (`app/src/main/java/ai/rojan/designlab/data/repository/TokenRepositoryImpl.kt:37-38`).
- `clearTokens()` هر دو کلید را کامل حذف می‌کند (خط 26-31).

### Logout — ✅ پاکسازی کامل (برای Customer scope)

**Evidence:** `app/src/main/java/ai/rojan/designlab/presentation/auth/AuthViewModel.kt:310-325`

```kotlin
fun logout() {
    sessionProvider.logout()
    tokenRepository.clearTokens()
    _currentUser.value = null
    _errorMessage.value = null
    _otpStep.value = CustomerOtpStep.EnteringPhone
    _identityContext.value = UiState.Loading
    viewModelScope.launch { authSessionRepository.clearPersonId() }
    _sessionState.value = sessionProvider.currentSession()
}
```
توکن‌ها، personId، و state در حافظه همگی پاک می‌شوند. (`activeSalonDataStore` عمداً پاک نمی‌شود — بررسی شد و تأیید شد که این DataStore **فقط توسط Manager/Reception** استفاده می‌شود، هیچ‌جای کد Customer آن را نمی‌خواند/نمی‌نویسد؛ بنابراین خارج از محدوده‌ی این audit است، نه یک نقص.)

### Session restore flow — ✅ تأیید شده

`AuthViewModel.restoreSession()` هرگز به یک personId ذخیره‌شده به‌تنهایی اعتماد نمی‌کند — همیشه از `backendAuthRepository.currentUser()` (که خودش refresh token را در صورت انقضای access token امتحان می‌کند) استفاده می‌کند و فقط در صورت موفقیت واقعی session را برقرار می‌کند (`AuthViewModel.kt:328-354`).

### Token leakage در Logcat — ✅ بدون یافته

- در **کل اپلیکیشن (همه فلیورها)** هیچ فراخوانی `Log.d/v/i/w/e`، `println`، یا `System.out.print` وجود ندارد — بررسی با جستجوی سراسری تأیید شد.
- تنها منبع لاگ شبکه، `HttpLoggingInterceptor` است که در release به‌طور قطعی `Level.NONE` است (نه فقط verbosity پایین‌تر) و حتی در دیباگ فقط `BASIC` (method+URL+response code) — هرگز header یا body، پس هیچ‌وقت Authorization/Bearer token یا کد OTP چاپ نمی‌شود (`di/BackendApiContainer.kt:284-293`).
- تنها استفاده از `BuildConfig.DEBUG` در کل اپ همین یک خط است — **هیچ backdoor یا auth-bypass دیباگ‌محور دیگری پیدا نشد.**

### یافته P1 — Backup Exclusion ناقص/ناهم‌خوان

**Evidence:**
- `app/src/main/res/xml/backup_rules.xml` و `app/src/main/res/xml/data_extraction_rules.xml`: فقط `datastore/role_preferences.preferences_pb` مستثنی شده است. کامنت خود همین فایل می‌گوید این استثنا برای جلوگیری از این سناریو است: *"اگر Auto Backup این فایل را به‌طور خاموش روی یک نصب تازه restore کند، اپ یک role واقعی ولی قدیمی را می‌خواند و به‌جای صفحه Welcome مستقیم به dashboard آن role می‌رود."*
- جستجوی سراسری کد نشان می‌دهد `RoleDataStore.kt`/`role_preferences` **دیگر هیچ‌جای کدبیس وجود ندارد** — این فایل قبلاً حذف شده (retired).
- در عوض، مکانیزم فعال فعلی session-restore یک DataStore دیگر است: `authSessionDataStore` با نام فایل `auth_session_preferences` (`data/local/AuthSessionDataStore.kt:21`) که `logged_in_person_id` را نگه می‌دارد — **همین دقیقاً همان چیزی است که استدلال کامنت بالا برایش نوشته شده بود، اما در لیست استثنا نیست.**
- `active_salon_preferences` (`data/local/ActiveSalonDataStore.kt:16`) هم مستثنی نشده، ولی چون خارج از scope کد Customer است، ریسک آن در عمل صفر است.

**تحلیل ریسک واقعی:** خود توکن‌های واقعی (access/refresh) در یک فایل جدا (`secure_token_preferences`) با Android Keystore رمزنگاری شده‌اند، و کلید Keystore معمولاً بعد از uninstall/reinstall یا انتقال به دستگاه جدید از بین می‌رود یا در دسترس نیست — پس حتی اگر این فایل هم backup شود، decrypt آن روی دستگاه/نصب جدید شکست می‌خورد (و `TokenRepositoryImpl` این شکست را امن مدیریت می‌کند). بنابراین این یافته **یک session hijack واقعی ایجاد نمی‌کند**، اما:
1. خود `personId` (یک شناسه‌ی هویتی) بدون محافظت به cloud backup گوگل می‌رود — نشتی حریم خصوصی محدود ولی واقعی.
2. دقیقاً همان کلاس باگ تجربه‌کاربری که استثنای role_preferences برایش نوشته شده بود (نصب تازه/دستگاه جدید یک session قدیمی را می‌خواند) می‌تواند دوباره رخ دهد، چون مکانیزم فعال محافظت نشده.

**Recommended Fix:** به هر دو فایل XML، مسیر `datastore/auth_session_preferences.preferences_pb` (و به‌صورت اختیاری `datastore/active_salon_preferences.preferences_pb`) اضافه شود؛ خط مربوط به `role_preferences` (که دیگر فایلی برایش وجود ندارد) قابل حذف است، هرچند حذفش صرفاً نظافت کد است نه یک نیاز امنیتی.

---

## 4. Network Security

**Evidence:** `app/src/main/java/ai/rojan/designlab/di/BackendApiContainer.kt`, `data/remote/NetworkConfig.kt`

| بررسی | نتیجه |
|---|---|
| HTTPS enforcement | `NetworkConfig.BASE_URL` از `BuildConfig.API_BASE_URL` می‌آید و برای production در build-time اجباراً باید `https://` باشد (بخش 2). برای `dev`/`staging` هیچ enforcement اجباری در runtime وجود ندارد (فقط توسعه‌دهنده مسئول URL محلی است) — قابل قبول برای build‌های غیرتوزیعی. |
| Cleartext traffic | هیچ `android:usesCleartextTraffic` یا Network Security Config در manifest نیست — یعنی رفتار پیش‌فرض پلتفرم اعمال می‌شود. چون `targetSdk=37` (بسیار بالاتر از 28) است، این پیش‌فرض **در کل بازه‌ی minSdk=24 تا بالا، cleartext را مسدود می‌کند** (این رفتار بر اساس targetSdk کامپایل‌شده تعیین می‌شود، نه نسخه OS دستگاه). یافته واقعی‌ای اینجا نیست، ولی **P2 برای شفافیت**: توصیه می‌شود `usesCleartextTraffic="false"` به‌صورت صریح نوشته شود تا این نیت به‌جای implicit، explicit باشد. |
| تعداد OkHttp client | **3 نمونه مستقل**: کلاینت اصلی authenticated (`buildAuthenticatedRetrofit`)، یک کلاینت plain فقط برای فراخوانی refresh داخل `TokenAuthenticator` (تا از recursion جلوگیری شود)، و یک کلاینت plain برای `PublicSalonApi` (endpoint بدون auth). هیچ‌کدام connection pool مشترک ندارند (`BackendApiContainer.kt:271-317`) — این یک یافته **عملکردی** است (در گزارش عملکرد قبلی این جلسه ثبت شده)، نه امنیتی مستقیم. |
| Interceptor ها | `AuthInterceptor` (اضافه‌کردن Bearer token)، `TokenAuthenticator` (refresh خودکار روی 401، با قفل همزمانی برای جلوگیری از race روی refresh-token rotation)، و `HttpLoggingInterceptor`. هیچ interceptor مشکوک/شخص‌ثالث دیگری نیست. |
| Logging interceptor در release | **قطعی خاموش** (`Level.NONE`)، تأیید شده در بخش 3. |
| Certificate pinning | **وجود ندارد** — اپ فقط به trust store سیستم‌عامل تکیه می‌کند. برای اکثر اپ‌های مصرفی قابل قبول است (به‌خصوص چون گواهی بک‌اند Let's Encrypt است، طبق audit قبلی بک‌اند)، ولی برای یک اپ حاوی اطلاعات پرداخت/رزرو، افزودن pinning یک سخت‌سازی اختیاری معقول است — **P2 (اختیاری، نه اجباری قبل از انتشار)**. |
| Timeout / certificate config | هیچ `connectTimeout`/`readTimeout`/`writeTimeout` صریحی روی هیچ‌کدام از 3 کلاینت ست نشده — یعنی مقادیر پیش‌فرض OkHttp (۱۰ ثانیه برای هرکدام) اعمال می‌شود. این یک ریسک امنیتی مستقیم نیست، صرفاً یک نکته‌ی سخت‌سازی سطح پایین (**اطلاعاتی، نه یک finding رسمی**). |

**Recommended Fix:** افزودن صریح `usesCleartextTraffic="false"` (یا یک Network Security Config حداقلی) برای شفافیت؛ بقیه موارد اختیاری/بعد از RC-1 قابل بررسی‌اند.

---

## 5. Privacy & Local Data

**Evidence:** `app/src/main/java/ai/rojan/designlab/ui/media/ImageDownscale.kt`, `screens/profile/ProfileScreen.kt`

### EXIF/GPS آواتار و کاور — ✅ کاملاً حذف می‌شود

`decodeResizeAndCompress` (استفاده‌شده در `ProfileScreen.kt:163,172` برای هر دو آواتار و کاور) عکس انتخاب‌شده را کامل به `Bitmap` decode می‌کند، فقط orientation را از EXIF می‌خواند و در پیکسل‌ها "می‌پزد"، سپس با `Bitmap.compress(JPEG)` دوباره encode می‌کند. چون Bitmap هیچ متادیتایی حمل نمی‌کند، **هر فیلد EXIF از جمله GPS به‌طور خودکار در همین re-encode حذف می‌شود** — این خودِ فایل هم در کامنت بالای خودش تصریح شده است (خط 20-28). این یک کنترل حریم‌خصوصی مثبت و از قبل درست پیاده‌سازی‌شده است.

### فایل موقت هنگام upload — ✅ بدون فایل موقت

کل pipeline decode→resize→compress در حافظه (`ByteArrayOutputStream`) انجام می‌شود؛ هیچ فایل موقتی روی دیسک نوشته نمی‌شود که نیاز به پاکسازی داشته باشد.

### Cache تصاویر (Coil) — ℹ️ اطلاعاتی، بدون یافته

اپ از `ImageLoader` پیش‌فرض Coil استفاده می‌کند (بدون کانفیگ سفارشی) — کش حافظه/دیسک استاندارد Coil در storage خصوصی اپ نگه‌داری می‌شود (نه world-readable). چون محتوای کش‌شده (لوگوی سالن، آواتار) از URLهای عمومی/غیرمحرمانه می‌آیند، این خودش نشتی محسوب نمی‌شود.

### پاکسازی هنگام logout — ✅ برای Customer scope کامل

قبلاً در بخش 3 پوشش داده شد.

### یافته P2 — نبود FLAG_SECURE

**Evidence:** جستجوی سراسری برای `FLAG_SECURE`/`setSecure`/`WindowManager.LayoutParams` در کل کدبیس **هیچ نتیجه‌ای نداد.**

این یعنی:
- امکان گرفتن screenshot یا screen-recording از هر صفحه‌ی اپ (از جمله صفحه‌ی ورود کد OTP) توسط اپ‌های دیگر (با دسترسی مربوطه) یا کاربر خودِ دستگاه بدون محدودیت وجود دارد.
- محتوای صفحه‌ی فعلی در thumbnail لیست "اپ‌های اخیر" (Recent Apps) سیستم‌عامل قابل مشاهده است.

**Recommended Fix:** افزودن `window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, ...)` حداقل روی صفحه‌ی ورود OTP (`AuthScreen`)؛ به‌عنوان یک رویکرد ساده‌تر و قابل دفاع، می‌توان همین flag را یک‌بار در `MainActivity.onCreate()` برای کل اپ فعال کرد، چون اپ محتوای شخصی (پروفایل، نوبت‌ها، شماره تلفن) هم نمایش می‌دهد.

---

## 6. Release Artifact Review

| بررسی | نتیجه | Evidence |
|---|---|---|
| ApplicationId | `ai.rojan.designlab` — بدون suffix برای فلیور customer (وراثت کامل از `defaultConfig`) | `app/build.gradle.kts:80,89` |
| VersionCode / VersionName | `1` / `"1.0.0"` — مناسب برای اولین انتشار | `app/build.gradle.kts:92-93` |
| Release signing | واقعی، v2+v3، از منبع خارج از git؛ production release بدون امضای واقعی اصلاً build نمی‌شود (بخش 2) | `app/build.gradle.kts:179-195, 258-289` |
| R8 rules | `proguard-rules.pro` مینیمال و دقیق — فقط `-keepattributes SourceFile,LineNumberTable` + `-renamesourcefileattribute SourceFile` (برای حفظ قابلیت deobfuscate کردن crash بدون افشای نام واقعی فایل‌های Kotlin در stack trace). هیچ `-keep class ** { *; }` یا قانون گسترده‌ای که obfuscation را خنثی کند وجود ندارد. کتابخانه‌های حساس (Retrofit، kotlinx.serialization، OkHttp، coroutines) قوانین R8 خودشان را bundle می‌کنند. | `app/proguard-rules.pro` |
| Proguard leakage | بدون یافته — هیچ secret/کلید در proguard rules هاردکد نشده. | همان فایل |
| Debug symbols | اپ هیچ کد native/NDK ندارد، پس نگرانی debug-symbol در سطح `.so` بی‌معناست. فایل `mapping.txt` (خروجی R8) در `app/build/outputs/mapping/<variant>/` نوشته می‌شود — یک artifact جداگانه‌ی build، **داخل APK/AAB بسته‌بندی نمی‌شود.** | استاندارد AGP، تأیید‌شده |
| فایل‌های secret در git | بررسی شد: `*.jks`، `keystore.properties`، `local.properties` در `.gitignore` هستند **و** با `git ls-files` تأیید شد که هیچ‌کدام واقعاً tracked نیستند. | `.gitignore:15,32-34,39` + `git ls-files` |

**Recommended Fix:** هیچ اقدام اجباری لازم نیست.

---

## 7. Dependency Security

**Evidence:** `gradle/libs.versions.toml`

| کتابخانه | نسخه | یافته |
|---|---|---|
| OkHttp | 4.12.0 | جستجوی وب (Snyk، cybersecurity-help.cz) هیچ CVE شناخته‌شده‌ای برای همین نسخه نشان نداد. ([Snyk](https://security.snyk.io/package/maven/com.squareup.okhttp3%3Aokhttp/4.12.0), [cybersecurity-help.cz](https://www.cybersecurity-help.cz/vdb/square_open_source/okhttp/4.12.0/)) |
| Retrofit | 2.11.0 | بدون CVE شناخته‌شده در بررسی. |
| Coil (compose) | 2.7.0، group id قدیمی `io.coil-kt` | **P2** — خط فعلی Coil نسخه‌ی 3.x است (`io.coil-kt.coil3`)، یعنی این وابستگی یک major version کامل عقب‌تر است. CVE مشخصی پیدا نشد، ولی از نظر maintenance/patch coverage توصیه به آپدیت می‌شود. ([GitHub coil-kt/coil](https://github.com/coil-kt/coil)) |
| kotlinx.serialization | 1.11.0 | نسخه‌ی به‌روز، بدون یافته. |
| androidx.exifinterface | 1.3.7 | **نکته‌ی مثبت**: کامنت خود کدبیس تصریح می‌کند این کتابخانه عمداً جایگزین `android.media.ExifInterface` شده چون آن یکی "known parsing vulnerabilities" روی نسخه‌های قدیمی‌تر اندروید دارد (`gradle/libs.versions.toml` کامنت بالای خط 36). |
| Kotlin coroutines | 1.9.0 | بدون یافته. |

### وابستگی‌هایی که permission/ریسک اضافه ایجاد می‌کنند
بررسی کامل `[libraries]` در `libs.versions.toml` نشان می‌دهد **هیچ SDK آنالیتیکس، تبلیغات، ردیاب شخص‌ثالث، یا کتابخانه‌ای که permission غیر از `INTERNET` نیاز داشته باشد وجود ندارد.** سطح وابستگی این اپ نسبت به یک اپ تجاری معمولی به‌طور قابل‌توجهی کوچک و کم‌ریسک است.

### یافته P2 — نبود بررسی خودکار وابستگی‌های قدیمی در CI
`app/build.gradle.kts:247` عمداً چک lint `NewerVersionAvailable` را خاموش می‌کند (با این استدلال که این چک به شبکه نیاز دارد و خروجی offline را non-deterministic می‌کند). این تصمیم برای پایداری build منطقی است، ولی نتیجه‌اش این است که **در حال حاضر هیچ مکانیزم خودکاری برای شناسایی وابستگی‌های قدیمی/آسیب‌پذیر در CI فعال نیست** (`GradleDependency` هنوز فعال است ولی فقط نسخه‌های شناخته‌شده‌ی offline را گزارش می‌کند، نه CVE واقعی).

**Recommended Fix:** آپدیت Coil به نسخه‌ی 3.x (خارج از محدوده‌ی این audit — نیاز به تست جداگانه دارد چون تغییر group id/API است)؛ افزودن یک ابزار مثل OWASP Dependency-Check یا Gradle Versions Plugin به‌صورت دوره‌ای (نه لزوماً در هر build) به CI.

---

## Release Decision

### آیا RC-1 از نظر امنیتی برای Release Candidate آماده است؟

**بله، مشروط.** هیچ **P0** پیدا نشد که مانع RC شود. معماری امنیتی هسته‌ای (رمزنگاری توکن، عدم نشت در لاگ، signing، R8، EXIF stripping) از قبل درست و محکم پیاده‌سازی شده است.

**قبل از انتشار عمومی (Play Store) توصیه می‌شود این دو مورد اصلاح شوند** (هر دو تغییرات کوچک و کم‌ریسک‌اند):
1. **P1** — افزودن `auth_session_preferences` به لیست استثنای backup (بخش 3). این یک تغییر یک‌خطی در دو فایل XML است.
2. **P2 با اولویت بالا** — افزودن `FLAG_SECURE` حداقل روی صفحه‌ی OTP (بخش 5).

**می‌توان بعد از RC-1 (یا در یک sprint سخت‌سازی جداگانه) انجام داد:**
- تصریح `usesCleartextTraffic="false"` (بخش 4) — رفتار فعلی از قبل درست است، فقط explicit نیست.
- آپدیت Coil به نسخه‌ی 3.x (بخش 7) — نیاز به تست رگرسیون جداگانه دارد، عجله‌ای نیست.
- افزودن certificate pinning (بخش 4) — سخت‌سازی اختیاری.
- افزودن بررسی خودکار وابستگی‌ها به CI (بخش 7).

**هیچ فایلی در این audit تغییر یا commit نشد.** پیاده‌سازی هرکدام از موارد بالا باید یک task جداگانه با validation کامل (compile/lint/assemble + در صورت لزوم تست روی دستگاه واقعی) باشد، نه بخشی از همین گزارش.
