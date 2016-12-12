---
docid: proguard
title: Using ProGuard with Fresco
layout: docs
permalink: /docs/proguard.html
prev: using-other-image-loaders.html
next: multiple-apks.html
---

Fresco's large download may seem intimidating, but it need not leave you with a large application. We strongly recommend use of the ProGuard tool.

You can download Fresco's ProGuard file, [proguard-fresco.pro](https://raw.githubusercontent.com/facebook/fresco/master/proguard-fresco.pro), and add it to your project.

### Android Studio / Gradle

Place a stanza like this in your `build.gradle` file.

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

### Eclipse

Edit your [proguard.cfg](http://developer.android.com/tools/help/proguard.html#enabling) file to include the contents of [proguard-fresco.pro](https://raw.githubusercontent.com/facebook/fresco/master/proguard-fresco.pro).
