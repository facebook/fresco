---
id: getting-started
title: Getting started with Fresco
layout: docs
permalink: /docs/getting-started.html
prev: index.html
next: concepts.html
---

If you just want to download an image and display it, showing a placeholder until it comes, use a [SimpleDraweeView](../javadoc/reference/com/facebook/drawee/view/SimpleDraweeView.html). 

For images from the network, you will need to to request Internet permission from your users. Add this line to your ```AndroidManifest.xml``` file:

```xml
  <uses-permission android:name="android.permission.INTERNET"/>
```

Near your application startup, before your app calls ```setContentView()```, initialize the Fresco class:

```java
Fresco.initialize(context);
```
    
In your XML, add a custom namespace to the top-level element:

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
Uri uri = Uri.parse("http://frescolib.org/static/fresco-logo.png");
SimpleDraweeView draweeView = (SimpleDraweeView) findViewById(R.id.my_image_view);
draweeView.setImageURI(uri);
```
and Fresco does the rest. 

The placeholder is shown until the image is ready. The image will be downloaded, cached, displayed, and cleared from memory when your view goes off-screen.
