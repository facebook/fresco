---
docid: index
title: Adding Fresco to your Project
layout: docs
permalink: /docs/index.html
next: getting-started.html
---

Here's how to add Fresco to your Android project.

### Android Studio or Gradle

Edit your `build.gradle` file. You must add the following line to the `dependencies` section:

```groovy
dependencies {
  // your app's other dependencies
  compile 'com.facebook.fresco:fresco:{{site.current_version}}'
}
```

The following optional modules may also be added, depending on the needs of your app.

```groovy
dependencies {
  // If your app supports Android versions before Ice Cream Sandwich (API level 14)
  compile 'com.facebook.fresco:animated-base-support:{{site.current_version}}'

  // For animated GIF support
  compile 'com.facebook.fresco:animated-gif:{{site.current_version}}'

  // For WebP support, including animated WebP
  compile 'com.facebook.fresco:animated-webp:{{site.current_version}}'
  compile 'com.facebook.fresco:webpsupport:{{site.current_version}}'

  // For WebP support, without animations
  compile 'com.facebook.fresco:webpsupport:{{site.current_version}}'

  // Provide the Android support library (you might already have this or a similar dependency)
  compile 'com.android.support:support-core-utils:{{site.support_library_version}}'
}
```

### Eclipse ADT

Download the [zip file](https://github.com/facebook/fresco/releases/download/v{{site.current_version}}/frescolib-v{{site.current_version}}.zip).

When you expand it, it will create a directory called 'frescolib'. Note the location of this directory.

1. From the File menu, choose Import.
2. Expand Android, select "Existing Android Code into Workspace", and click Next.
3. Click Browse, navigate to the frescolib directory, and click OK.
4. A number of projects should be added. Make sure that at least the following are checked: `drawee`, `fbcore`, `fresco`, `imagepipeline`, `imagepipeline-base`. The others are optional depending on your app's needs similar to the breakdown above for Gradle.
5. Right-click (Ctrl-click on Mac) on your project and choose Properties, then click Android.
6. Click the Add button in the bottom right, select Fresco, and click OK, then click OK again.

You should now be able to build your app with Fresco.

If you want to use OkHttp as the network layer, see the [separate instructions](using-other-network-layers.html#_).

If you get a `Jar Mismatch` warning about `android-support-v4.jar`, delete the one in `frescolib/imagepipeline/libs.`
