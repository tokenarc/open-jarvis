# Open Jarvis ProGuard Rules (R8 Full Mode)

# Keep Room entities
-keep class com.openjarvis.graphify.nodes.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep AccessibilityService
-keep class com.openjarvis.accessibility.JarvisAccessibilityService { *; }
-keep class * extends android.accessibilityservice.AccessibilityService
-keep class * extends android.view.accessibility.AccessibilityNodeInfo

# Keep Graphify
-keep class com.openjarvis.graphify.** { *; }

# Keep model classes
-keep class com.openjarvis.** { *; }

# Coroutines
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
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# MLKit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Security Crypto
-keep class androidx.security.crypto.** { *; }

# Work Manager
-keep class androidx.work.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# R8 optimizations
-allowaccessmodification
-repackageclasses
-mergeinterfacesaggressively
-optimizationpasses 5

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}