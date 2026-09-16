# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve source file name + line numbers in release stack traces so
# crash reports stay deobfuscatable (the mapping file is in
# app/build/outputs/mapping/<variant>/mapping.txt). Standard release
# practice; no classes kept, only attributes retained.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# STATUS (Sprint 5A-2): R8 minification + resource shrinking are now ON
# for release builds (app/build.gradle.kts buildTypes.release:
# isMinifyEnabled / isShrinkResources / proguardFiles). This file is
# wired in via proguardFiles alongside proguard-android-optimize.txt.
#
# No project keep rules are needed beyond the attributes above:
#   - App code has zero reflection, no Class.forName / ::class.java (other
#     than retrofit.create(...), covered below), no dynamic class or
#     resource loading, no WebView/JS interface, no @Keep / @Parcelize,
#     no manual-DI reflection (BackendApiContainer is plain constructors).
#   - Retrofit 2.11.0 ships META-INF/proguard/retrofit2.pro (keeps
#     @retrofit2.http.* service interfaces, Signature/InnerClasses,
#     Continuation, Response).
#   - kotlinx-serialization-core 1.11.0 ships
#     META-INF/com.android.tools/r8/kotlinx-serialization-*.pro (keeps
#     $serializer, serializer(), INSTANCE, @Serializable retention). All
#     61 @Serializable types are plain DTOs — no polymorphic/sealed/
#     contextual serialization, no SerializersModule.
#   - OkHttp 4.12.0 and kotlinx-coroutines 1.9.0 ship their own bundled
#     R8 rules; Compose and Coil 2.7.0 are R8-safe without extra rules.
# R8 is run in full mode; add narrowly-scoped rules here only if a real
# build/runtime failure proves one is required.

# customerProductionRelease launch crash (2026-09-14): a real device install of
# the signed release APK hit a message-less java.lang.ClassCastException inside
# BackendApiContainer.<init> (retrofit.create(BookingApi::class.java)), fully
# retraced via mapping.txt. Root cause: R8's singleton/field merging had folded
# retrofit2.BuiltInConverters' inner converter singletons together with
# okhttp3.CookieJar's and okhttp3.Dns's default-instance singletons into one
# unrelated merged class shared with several Compose/coroutines singletons
# (confirmed in configuration.txt: neither Retrofit's nor OkHttp's own bundled
# consumer rules, nor this file, kept any of these three). Narrowly scoped to
# just these three inner-class families - not a broad keep, not a shrinking
# disable.
-keep class retrofit2.BuiltInConverters$* { *; }
-keep class okhttp3.CookieJar$* { *; }
-keep class okhttp3.Dns$* { *; }

# Isolated experiment (2026-09-14, same investigation): the three rules above
# did NOT resolve the crash - confirmed via a real device retest, and via a
# fresh mapping.txt showing all three families cleanly un-merged while the
# identical crash persisted at the identical BackendApiContainer.kt:161. The
# same merged host class (shared with BackendApiContainerHolder) still also
# carries kotlinx.serialization.json.internal.JsonPath$RedactedKey/$Tombstone
# - singletons actually touched by this file's own `Json{...}.asConverterFactory(...)`
# field. Testing this one family in isolation, deliberately not combined with
# any other candidate rule, to attribute the result unambiguously.
-keep class kotlinx.serialization.json.internal.JsonPath$* { *; }

# Confirmed root cause (2026-09-14, same investigation): the two experiments
# above also did not change the crash. Direct `dexdump` disassembly of
# BackendApiContainer.<init> in the release APK showed R8 had replaced the
# check-cast for `retrofit.create(SalonBookingApi::class.java)` (the second
# argument to BookingRepositoryImpl(...) at BackendApiContainer.kt:161) with
# an unconditional `new ClassCastException(); throw` - no check-cast
# instruction present at all, which is why the exception always carried no
# message. Every other retrofit.create(...) call in the same constructor,
# including the immediately preceding one for BookingApi, keeps its real
# check-cast intact. Retrofit's own bundled `-if interface * { @retrofit2.http.*
# <methods>; } -keep,allowobfuscation interface <1>` rule protects every other
# Retrofit interface here but evidently did not take effect for this one -
# SalonBookingApi is real, reachable code (BookingRepositoryImpl.kt calls
# .bookings() on it), not dead code. Keeping only this one interface directly.
-keep interface ai.rojan.designlab.data.remote.SalonBookingApi { *; }

# Follow-up (2026-09-15, same investigation): rebuilding with only the
# SalonBookingApi rule above fixed that specific cast (confirmed via dexdump -
# a real check-cast and a succeeding BookingRepositoryImpl.<init> call are now
# present at BackendApiContainer.kt:161), but direct disassembly of the same
# rebuilt APK showed the identical defect had simply moved to the next
# retrofit.create(...) call in the same constructor:
# SalonCustomerApi (BackendApiContainer.kt:168, used by
# SalonCustomerRepositoryImpl) - same message-less unconditional
# `new ClassCastException(); throw`, no check-cast at all. Both affected
# interfaces are single-method, Retrofit-proxy-only contracts under this
# app's own ai.rojan.designlab.data.remote package. Rather than continuing to
# patch one interface at a time as R8 hits each one, this covers the whole
# closed, app-owned package in one narrowly-scoped rule - no third-party
# class, no other package.
-keep interface ai.rojan.designlab.data.remote.** { *; }
