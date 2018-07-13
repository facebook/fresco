# Third-party libraries for samples
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

-keep class com.facebook.drawee.generic.GenericDraweeHierarchy {
    boolean hasImage();
}