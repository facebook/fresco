
Fresco's binary download consists of four .aar (Android archive) files. Decompressed, these take up a total space of:

### Using ProGuard with Fresco


### Using product flavors

Fresco is written mostly in Java, but there is some C++ as well. C++ code has to be compiled for each of the CPUs Android can run on. Currently, Fresco supports three CPUs.

1. `armeabiv-v7a`: Version 7 or higher of the ARM processor. Most Android phones released since 2011 are using this.
1. `armeabi`: Older phones using v5 or v6 of the ARM processor. 
1. `x86`: Mostly used by emulators running on desktops or laptops.

Fresco's binary download has copies of native .so files for all three platforms. You can reduce the size of your app considerably by creating separate APKs for each processor type.

If your application is not used by devices running Android 2.3 (Gingerbread), you will not need the `armeabi` flavor. If you do not use emulators to test your app, you will not need the `x86` flavor.

#### Using product flavors in Android Studio / Gradle

Edit your `build.gradle` file as follows:

```groovy
android {
  // rest of your app's logic
  productFlavors {
    armv7 {
      ndk {
        abiFilter "armeabi-v7a"
      }
    }
    arm {
      ndk {
        abiFilter "armeabi"
      }
    }
    x86 {
      ndk {
        abiFilter "x86"
      }
    }
  }
}
```

See the [Android Gradle documentation](http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Product-flavors) for more details on how product flavors work.

You can leave out the `arm` flavor if your app requires Android 3.0 or higher to run.

#### Using product flavors in Eclipse 

By default, Eclipse produces a single APK for all CPU flavors. To separate them requires more complexity and more effort than in Gradle.

In addition to the standard zip file, Fresco offers a [multi-APK zip file](https://github.com/facebook/fresco/releases/download/v{{site.current_version}}/frescolib-v{{site.current_version}}-multi.zip). However, it is not enough to download it - you must alter your project too.

1. Follow [Android's instructions]() for converting your code into a multi-APK project.
2. Each flavored project should depend on the corresponding `fresco-<flavor>` project. (If you are using OkHttp, also depend on the `imagepipeline-okhttp-<flavor>` project.)


#### Uploading your app to the Play Store

You can either upload a single APK that all users can download, or separate, smaller APKs for each CPU. The [Android documentation](http://developer.android.com/google/play/publishing/multiple-apks.html) provides instructions on how to do this.