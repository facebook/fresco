---
docid: building-from-source
title: Building from Source
layout: docs
permalink: /docs/building-from-source.html
prev: multiple-apks.html
next: sample-code.html
---

You should only build from source if you need to modify Fresco code itself. Most applications should simply [include](index.html#_) Fresco in their project.

### Prerequisites

The following tools must be installed on your system in order to build Fresco:

1. The Android [SDK](https://developer.android.com/sdk/index.html#Other)
2. From the Android SDK Manager, install/upgrade the latest Support Library **and** Support Repository. Both are found in the Extras section.
2. The Android [NDK](https://developer.android.com/tools/sdk/ndk/index.html). Version 10c or later is required.
3. The [git](http://git-scm.com/) version control system.

You don't need to download Gradle itself; the build scripts or Android Studio will do that for you.

Fresco does not support source builds with Eclipse, Ant, or Maven. We do not plan to ever add such support. Eclipse users can [include](index.html#eclipse-adt) Fresco in their projects instead.

### Configuring Gradle

Both command-line and Android Studio users need to edit the `gradle.properties` file. This is normally located in your home directory, in a subdirectory called `.gradle`. If it is not already there, create it.

On Unix-like systems, including Mac OS X, add this line:

```groovy
ndk.path=/path/to/android_ndk/r10e
```

On Windows systems, add this line:

```groovy
ndk.path=C\:\\path\\to\\android_ndk\\r10e
```

On *both* platforms, add these lines:

```groovy
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

Windows' backslashes and colons need to be escaped in order for Gradle to read them correctly.

### Getting the source

```sh
git clone https://github.com/facebook/fresco.git
```

This will create a directory `fresco` where the code will live.

### Building from the Command Line

On Unix-like systems, `cd` to the directory containing Fresco. Run the following command:

```sh
./gradlew build
```

On Windows, open a Command Prompt, `cd` to the directory containing Fresco, and type in this command:

```bat
gradlew.bat build
```

### Building from Android Studio

From Android Studio's Quick Start dialog, click Import Project. Navigate to the directory containing Fresco and click on the `build.gradle` file.

Android Studio should build Fresco automatically.

### Offline builds

The first time you build Fresco, your computer must be connected to the Internet. Incremental builds can use Gradle's `--offline` option.

### Troubleshooting

> Could not find com.android.support:...:x.x.x.

Make sure your Support Repository is up to date (see Prerequisites above).

### Windows support

We try our best to support building on Windows but we can't commit to it. We do not have a Windows build set up on our CI servers and none of us is using a Windows computer so the builds can break without us noticing it.

Please raise github issues if the Windows build is broken or submit a pull request with the fix. We do our best but we'd like the community's help to keep this up to date.


### Contributing code upstream

Please see our [CONTRIBUTING](https://github.com/facebook/fresco/blob/master/CONTRIBUTING.md) page.
