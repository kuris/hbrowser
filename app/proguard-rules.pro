# HBrowser ProGuard Rules

# Keep Room entities
-keep class kr.chriskim.hbrowser.core.database.** { *; }
-keep class kr.chriskim.hbrowser.core.model.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Hono annotations (Room)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Suppress warnings
-dontwarn okhttp3.**
-dontwarn org.bouncycastle.**
