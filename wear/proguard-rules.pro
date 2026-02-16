# Wear OS ProGuard rules

# Keep Wear OS related classes
-keep class androidx.wear.** { *; }

# Keep Media3 related classes
-keep class androidx.media3.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Compose related
-keep class androidx.compose.** { *; }

# Keep model classes
-keep class de.danoeh.antennapod.model.** { *; }

# Keep EventBus subscribers
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
