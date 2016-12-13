---
docid: getting-started
title: Getting started with Fresco
layout: docs
permalink: /docs/getting-started.html
prev: index.html
next: concepts.html
---

If you just want to download an image and display it, showing a placeholder until it comes, use a [SimpleDraweeView](../javadoc/reference/com/facebook/drawee/view/SimpleDraweeView.html).

But before that, you need to initialize the `Fresco` class. You should only call `Fresco.initialize` once. Your Application class would be a good place. Doing it in each Activity is wrong.

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

You need to specify your application class in the XML. For images from the network, you will also need to request Internet permission from your users. Your ```AndroidManifest.xml``` should look something like this:

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


In your layout XML, add a custom namespace to the top-level element:

```xml
<!-- Any valid element will do here -->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:fresco="http://schemas.android.com/apk/res-auto"
    android:layout_height="match_parent"
    android:layout_width="match_parent">
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
