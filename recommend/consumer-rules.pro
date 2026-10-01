# =============================================================
# recommend Consumer ProGuard Rules
# RecommendHelper 初始化 + JSON 解析
# =============================================================
-keepattributes Signature
-keepattributes *Annotation*

# Recommend 数据实体 —— JSON 反序列化
-keep class app.allever.android.lib.recommend.data.** { *; }

# Glide 引用
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
