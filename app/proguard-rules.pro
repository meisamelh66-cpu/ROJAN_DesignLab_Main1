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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# NOTE (Full Engineering Audit, P1-1): this file is not yet referenced by
# app/build.gradle.kts, since release optimization is explicitly disabled
# (buildTypes.release.optimization.enable = false) - minification/R8 was
# never turned on, so there was nothing for this file to configure. This
# file exists so it's ready when that's enabled, but wiring it in and
# actually turning on minification is a real testing decision (verifying
# nothing gets stripped that shouldn't be) that can't be safely made
# without a real build - deliberately left for a future, tested pass
# rather than flipped blindly here.
