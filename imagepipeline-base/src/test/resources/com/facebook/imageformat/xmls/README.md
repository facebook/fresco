# XMLs

## Context

Android compiles raw XML to binary XML during app build as layout inflation at runtime does not support parsing raw XML files. As a result, Fresco only supports loading binary XML files since it relies on the Android to perform layout inflation. Therefore, we need to mock the build step that performs this compilation with the Android packaging tool. This directory houses raw, uncompiled files and their compiled counterparts in respective directories so we can test against them.

## How to add/update assets

These instructions are for POSIX devices. If you are on Windows, you will have to manually run these steps or use Android Studio to generate an APK

1. Install Android's command-line tools by [following these instructions](https://developer.android.com/tools). Make sure you have `$ANDROID_HOME` set in your PATH by running `echo $ANDROID_HOME`
1. Install the latest packages of Android's build tools and platform. For example, you can run `sdkmanager "build-tools;34.0.0"` or `sdkmanager "platforms;android-33"`. [More instructions for sdkmanager can be found here](https://developer.android.com/tools/sdkmanager)
1. Run `./convert.sh` to automatically compile and extract resources into the `compiled` folder. It will use the latest available version of your build-tools and platforms.
1. That's it!
