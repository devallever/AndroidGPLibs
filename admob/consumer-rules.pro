# =============================================================
# admob Consumer ProGuard Rules
# AdMob SDK + UMP (同意管理平台) 必须完整 keep
# =============================================================

# AdMob SDK —— 不能只 keep public！内部类用了大量反射
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.gms.ads.mediation.** { *; }

# UMP (User Messaging Platform) —— GDPR 同意管理
-keep class com.google.android.ump.** { *; }

# AdMob 回调接口 —— 反射创建
-keep class * extends com.google.android.gms.ads.AdListener { *; }
-keep class * extends com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback { *; }
-keep class * extends com.google.android.gms.ads.FullScreenContentCallback { *; }

# Kotlin 协程 + lifecycle
-keep class androidx.lifecycle.** { *; }

# Glide (admob 原生广告加载图用)
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule

# 通用
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.android.gms.**
-dontwarn com.google.ads.**
-dontwarn com.google.android.ump.**
