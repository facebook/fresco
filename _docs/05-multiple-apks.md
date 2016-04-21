---
docid: multiple-apks
title: Shipping Multiple APKs
layout: docs
permalink: /docs/multiple-apks.html
prev: proguard.html
next: building-from-source.html
---

Fresco is written mostly in Java, but there is some C++ as well. C++ code has to be compiled for each of the CPU types (called "ABIs") Android can run on. Currently, Fresco supports five ABIs.

1. `armeabiv-v7a`: Version 7 or higher of the ARM processor. Most Android phones released from 2011-15 are using this.
2. `arm64-v8a`: 64-bit ARM processors. Found on new devices, like the Samsung Galaxy S6.
1. `armeabi`: Older phones using v5 or v6 of the ARM processor.
1. `x86`: Mostly used by tablets, and by emulators.
2. `x86_64`: Used by 64-bit tablets.

Fresco's binary download has copies of native `.so` files for all five platforms. You can reduce the size of your app considerably by creating separate APKs for each processor type.

If your application is not used by devices running Android 2.3 (Gingerbread), you will not need the `armeabi` flavor.

### Android Studio / Gradle

Edit your `build.gradle` file as follows:

```groovy
android {
  // rest of your app's logic
  splits {
    abi {
        enable true
        reset()
        include 'x86', 'x86_64', 'arm64-v8a', 'armeabi-v7a', 'armeabi'
        universalApk false
    }
  }
}
```

See the [Android Gradle documentation](http://tools.android.com/tech-docs/new-build-system/user-guide/apk-splits) for more details on how splits work.

### Eclipse

By default, Eclipse produces a single APK for all CPU flavors. To separate them requires more complexity and effort than in Gradle.

Instead of Fresco's standard zip file for eclipse, download the [multi-APK zip file](https://github.com/facebook/fresco/releases/download/v{{site.current_version}}/frescolib-v{{site.current_version}}-multi.zip). However, it is not enough to download it - you must alter your project too.

1. Follow [Android's instructions](http://developer.android.com/training/multiple-apks/api.html) for converting your code into a multi-APK project. You can use the same `AndroidManifest.xml` file for each one.
2. Each flavored project should depend on the corresponding `fresco-<flavor>` project. (If you are using OkHttp, also depend on the `imagepipeline-okhttp-<flavor>` project.)


### Uploading to the Play Store

You can either upload a single APK that all users can download, or separate, smaller APKs for each CPU. The [Play Store documentation](http://developer.android.com/google/play/publishing/multiple-apks.html) provides instructions on how to do this.
