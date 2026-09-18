# Room Database Rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
