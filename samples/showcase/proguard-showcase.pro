# Third-party libraries for samples
-keep class com.caverock.androidsvg.** { *; }
-keep class com.facebook.flipper.** { *; }
-keep class com.facebook.jni.** { *; }

-dontwarn com.caverock.androidsvg.**

-keep class com.facebook.drawee.generic.GenericDraweeHierarchy {
    boolean hasImage();
}

-dontwarn com.squareup.leakcanary.**

-dontwarn android.text.StaticLayout
-dontwarn android.view.DisplayList
-dontwarn android.view.RenderNode
-dontwarn android.view.DisplayListCanvas
-dontwarn android.view.HardwareCanvas
-dontwarn android.view.GLES20DisplayList

-dontwarn com.facebook.fbui.**
-dontwarn com.facebook.litho.**

-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**

-dontwarn com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
-dontwarn com.facebook.proguard.annotations.DoNotStrip
-dontwarn org.slf4j.impl.StaticLoggerBinder
