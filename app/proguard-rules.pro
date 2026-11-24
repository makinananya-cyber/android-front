# kotlinx.serialization keeps its generated serializers via its own consumer rules.
# Keep the payload classes' names readable in crash reports.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.hisaabwise.**$$serializer { *; }
-keepclassmembers class com.hisaabwise.** {
    *** Companion;
}
-keepclasseswithmembers class com.hisaabwise.** {
    kotlinx.serialization.KSerializer serializer(...);
}
