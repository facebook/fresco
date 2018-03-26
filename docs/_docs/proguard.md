---
docid: proguard
title: Shipping Your App with Fresco
layout: docs
redirect_from: /docs/proguard.html
permalink: /docs/shipping.html
---

Fresco's large size may seem intimidating, but it need not leave you with a large app. We strongly recommend use of the ProGuard tool as well as building split APKs to keep your app small.

### ProGuard

You can download Fresco's ProGuard file, [proguard-fresco.pro](https://raw.githubusercontent.com/facebook/fresco/master/proguard-fresco.pro), and add it to your project.

To enable ProGuard and configure it to use the Fresco configuration, modify your `build.gradle` file to include the lines contained in the `release` section below.

```groovy
android {
  buildTypes {
    release {
      minifyEnabled true
      proguardFiles getDefaultProguardFile('proguard-android.txt'),
        'proguard-fresco.pro'
    }
  }
}
```

### Build Multiple APKs

Fresco is written mostly in Java, but there is some C++ as well. C++ code has to be compiled for each of the CPU types (called "ABIs") Android can run on. Currently, Fresco supports five ABIs.

1. `armeabiv-v7a`: Version 7 or higher of the ARM processor. Most Android phones released from 2011-15 are using this.
2. `arm64-v8a`: 64-bit ARM processors. Found on new devices, like the Samsung Galaxy S6.
1. `armeabi`: Older phones using v5 or v6 of the ARM processor.
1. `x86`: Mostly used by tablets, and by emulators.
2. `x86_64`: Used by 64-bit tablets.

Fresco's binary download has copies of native `.so` files for all five platforms. You can reduce the size of your app considerably by creating separate APKs for each processor type.

If your app does not support Android 2.3 (Gingerbread) you will not need the `armeabi` flavor.

To enable multiple APKs, add the `splits` section below to the `android` section of your `build.gradle` file.

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

See the [Android publishing documentation](https://developer.android.com/google/play/publishing/multiple-apks.html) for more details on how splits work.
