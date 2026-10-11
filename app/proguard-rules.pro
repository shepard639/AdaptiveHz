# Proguard & R8 rules for AdaptiveHz

# Keep data models and enums
-keep class com.antigravity.adaptivehz.data.** { *; }
-keep class com.antigravity.adaptivehz.core.DisplayModeInfo { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static kotlin.enums.EnumEntries getEntries();
}

# Keep Services and Receivers
-keep class com.antigravity.adaptivehz.service.** extends android.app.Service { *; }
-keep class com.antigravity.adaptivehz.receiver.** extends android.content.BroadcastReceiver { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }
-keep class * extends android.service.quicksettings.TileService { *; }

# Keep DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }

# Jetpack Compose and Coroutines optimization
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.compose.**

# Keep Shizuku classes and reflection
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**
