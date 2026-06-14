# 蒜来宝 ProGuard 规则
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.whycun.garlicapp.data.remote.** { *; }
-keep class com.whycun.garlicapp.data.local.entity.** { *; }
