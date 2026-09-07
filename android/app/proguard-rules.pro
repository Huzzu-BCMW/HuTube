# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class androidx.media3.** { *; }
