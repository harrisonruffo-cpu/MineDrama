# ProGuard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class io.appwrite.** { *; }
-keep interface io.appwrite.** { *; }
