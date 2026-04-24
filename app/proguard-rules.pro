# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.openjarvis.graphify.nodes.** { *; }

# Keep AccessibilityService
-keep class com.openjarvis.accessibility.JarvisAccessibilityService { *; }

# Keep Graphify
-keep class com.openjarvis.graphify.** { *; }

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}