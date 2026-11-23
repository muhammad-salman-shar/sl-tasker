# Aggressive Obfuscation & Optimization
-obfuscationdictionary obf-dict.txt
-classobfuscationdictionary obf-dict.txt
-packageobfuscationdictionary obf-dict.txt
-repackageclasses 'o'
-allowaccessmodification

# Strip all debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Room Database Rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# WorkManager & BroadcastReceivers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Google Play Billing Library
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.**


# ML Kit Barcode, CameraX, and ZXing
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class com.google.zxing.** { *; }
