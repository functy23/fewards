-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature,Exception
-dontwarn kotlinx.serialization.**
-keep,includedescriptorclasses class com.functy.fewards.**$$serializer { *; }
-keepclassmembers class com.functy.fewards.** {
    *** Companion;
}
