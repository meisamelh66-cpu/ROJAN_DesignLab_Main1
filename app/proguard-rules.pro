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
