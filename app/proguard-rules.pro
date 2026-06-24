# Kotlin
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Coil
-dontwarn coil.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Data models — keep for serialization safety
-keep class com.melato.shop.data.model.** { *; }

# Lifecycle ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(); }
