---
docid: index
title: Getting Started with Fresco
layout: docs
permalink: /docs/index.html
---

This Guide will walk you through the steps needed to start using Fresco in your app, including loading your first image.

### 1. Update Gradle configuration

Edit your `build.gradle` file. You must add the following line to the `dependencies` section:

```groovy
dependencies {
  // your app's other dependencies
  implementation 'com.facebook.fresco:fresco:{{site.current_version}}'
}
```

The following optional modules may also be added, depending on the needs of your app.

```groovy
dependencies {

  // For animated GIF support
  implementation 'com.facebook.fresco:animated-gif:{{site.current_version}}'

  // For WebP support, including animated WebP
  implementation 'com.facebook.fresco:animated-webp:{{site.current_version}}'
  implementation 'com.facebook.fresco:webpsupport:{{site.current_version}}'

  // For WebP support, without animations
  implementation 'com.facebook.fresco:webpsupport:{{site.current_version}}'

  // Provide the Android support library (you might already have this or a similar dependency)
  implementation 'com.android.support:support-core-utils:{{site.support_library_version}}'
}
```

### 2. Initialize Fresco & Declare Permissions

Fresco needs to be initialized. You should only do this 1 time, so placing the initialization in your Application is a good idea. An example for this would be:

```java
[MyApplication.java]
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
    }
}
```

*NOTE:* Remember to also declare you Application class in the ```AndroidManifest.xml``` as well as add the required permissions. In most cases you will need the INTERNET permission.

```xml
  <manifest
    ...
    >
    <uses-permission android:name="android.permission.INTERNET" />
    <application
      ...
      android:label="@string/app_name"
      android:name=".MyApplication"
      >
      ...
    </application>
    ...
  </manifest>
```

### 3. Create a Layout

In your layout XML, add a custom namespace to the top-level element. This is needed to access the custom `fresco:` attributes which allows you to control how the image is loaded and displayed.

```xml
<!-- Any valid element will do here -->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:fresco="http://schemas.android.com/apk/res-auto"
    android:layout_height="match_parent"
    android:layout_width="match_parent"
    >
```

Then add the ```SimpleDraweeView``` to the layout:

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="130dp"
    android:layout_height="130dp"
    fresco:placeholderImage="@drawable/my_drawable"
    />
```

To show an image, you need only do this:

```java
Uri uri = Uri.parse("https://raw.githubusercontent.com/facebook/fresco/master/docs/static/logo.png");
SimpleDraweeView draweeView = (SimpleDraweeView) findViewById(R.id.my_image_view);
draweeView.setImageURI(uri);
```
and Fresco does the rest.

The placeholder is shown until the image is ready. The image will be downloaded, cached, displayed, and cleared from memory when your view goes off-screen.
