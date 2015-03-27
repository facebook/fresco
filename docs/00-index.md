---
id: getting-started
title: Getting started with Fresco
layout: docs
permalink: /docs/index.html
prev: download-fresco.html
next: concepts.html
---

If you just want to download an image and display it, showing a placeholder until it comes, use a [SimpleDraweeView](../javadoc/reference/com/facebook/drawee/view/SimpleDraweeView.html). 

Near your application startup, initialize the classes:

```java
Fresco.initialize(context);
```
    
In your XML, add a custom namespace to the top-level element:

```xml
<!-- Any valid element will do here -->
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:fresco="http://schemas.android.com/apk/res-auto">
```

Then add the view itself:

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="20dp"
    android:layout_height="20dp"
    fresco:placeholderImage="@drawable/my_drawable"
  />
```

To show an image, you need only do this:

```java
draweeView.setImageURI("http://site.com/uri");
```
    
and Fresco does the rest. 

The placeholder is shown until the image is ready. The image will be downloaded, cached, displayed, and cleared from memory when your view goes off-screen.


