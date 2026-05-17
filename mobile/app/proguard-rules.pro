# sqldpass mobile — release ProGuard/R8 rules.
# AGP 의 proguard-android-optimize.txt 가 기본 압축·난독화 처리. 여기엔 라이브러리별 keep 만.

# ---- Moshi (Kotlin reflective) ----
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass *;
}
# Moshi KotlinJsonAdapterFactory 는 리플렉션으로 data class 생성자를 호출하므로
# data 클래스 자체와 생성자 파라미터를 보존해야 한다.
-keep,allowobfuscation,allowshrinking class kotlin.Metadata { *; }
-keep class com.sqldpass.app.data.** { *; }

# ---- Retrofit / OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ---- Kotlinx Coroutines ----
-dontwarn kotlinx.coroutines.**

# ---- Google Sign-In / Play Services ----
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ---- Play Billing ----
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ---- AndroidX Compose / Lifecycle ----
-dontwarn androidx.compose.**
-dontwarn androidx.lifecycle.**

# ---- Logging strip ----
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
