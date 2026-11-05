// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Pass 62 (ROJAN_Backend already proved this exact plugin/config shape - mirrored here, not
    // reinvented): report-only OWASP Dependency-Check baseline. Android had no dependency-
    // vulnerability scanning at all before this (confirmed: zero prior reference to this plugin
    // anywhere in this file or app/build.gradle.kts).
    //
    // apply false: root-cause investigation (bytecode inspection of the plugin's own Analyze.
    // scanDependencies, confirmed real via javap) found that dependencyCheckAnalyze only ever
    // calls processConfigurations(this.getProject(), engine) - exactly once, on whichever
    // project the plugin is applied to, with no allprojects/subprojects traversal anywhere in
    // the plugin itself (confirmed in DependencyCheckPlugin.apply too). Applied only here at
    // root (which has no real Android/Kotlin plugin and so no real configurations), every past
    // scan found 0 dependencies - never actually reaching :app's real configurations. Moved to
    // app/build.gradle.kts, the real project, below.
    id("org.owasp.dependencycheck") version "13.0.0" apply false
}
