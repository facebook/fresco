---
id: building-from-source
title: Building from Source
layout: docs
permalink: /docs/building-from-source.html
prev: using-other-image-loaders.html
---

You should only build from source if you need to modify Fresco code itself. Most applications should simply [include](index.html) Fresco in their project.

### Prerequisites

The following tools must be installed on your system in order to build Fresco:

1. The Android [SDK](https://developer.android.com/sdk/index.html#Other)
2. From the Android SDK Manager, install the Support Library **and** the Support Repository. Both are found in the Extras section.
2. The Android [NDK](https://developer.android.com/tools/sdk/ndk/index.html). Version 10c or later is required.
3. The [git](http://git-scm.com/) version control system.

You don't need to download Gradle itself; the build scripts or Android Studio will do that for you.

Fresco does not support source builds with Eclipse, Ant, or Maven. We do not plan to ever add such support. Maven projects can still [include](index.html) Fresco, and we hope to later add Eclipse and Ant support.

### Configuring Gradle

Both command-line and Android Studio users need to edit the `gradle.properties` file. This is normally located in your home directory, in a subdirectory called `.gradle`. If it is not already there, create it.

On Unix-like systems, including Mac OS X, add a line like this:

```groovy
ndk.path=/path/to/android_ndk/r10d
```

On Windows systems, add a line like this:

```groovy
ndk.path=C\:\\path\\to\\android_ndk\\r10d
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

### Contributing code upstream

Please see our [CONTRIBUTING](https://github.com/facebook/fresco/blob/master/CONTRIBUTING.md) page.
