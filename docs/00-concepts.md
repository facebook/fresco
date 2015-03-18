---
id: concepts
title: Concepts
layout: docs
permalink: /docs/concepts.html
prev: index.html
next: supported-uris.html
---

## Drawees

Drawees are spaces in which images are rendered. These are made up of three components, like an Model-View-Controller framework.

### DraweeView

Descended from the Android [View](http://developer.android.com/reference/android/view/View.html) class. 

Most apps should use the `SimpleDraweeView` class. Place these in your application using XML or Java code. Set the URI to load with the `setImageURI` method, as explained in the [Getting Started](index.html) page.

You can [customize its appearance in XML](using-drawees-xml.html).

### DraweeHierarchy

This is the hierarchy of Android [Drawable](http://developer.android.com/reference/android/widget/Drawable.html) objects that will actually render your content. Think of it as the Model in an MVC.

If you need to [customize your image's appearance in Java](using-drawees-code.html), this is the class you will deal with.

### DraweeController

The `DraweeController` is the class responsible for actually dealing with the underlying image loader - whether Fresco's own image pipeline, or another.

If you need something more than a single URI to specify the image you want to display, you will need an instance of this class.

### DraweeControllerBuilder

`DraweeControllers` are immutable once constructed. They are [built](using-controllerbuilder.html) using the Builder pattern.

### Listeners

One use of a builder is to specify a [Listener](listening-download-events.html) to execute code upon the arrival, full or partial, of image data from the server.

## The Image Pipeline

Behind the scenes, Fresco's image pipeline deals with the work done in getting an image. It fetches from the network, a local file, a content provider, or a local resource. It keeps a cache of compressed images on local storage, and a second cache of decompressed images in memory.

The image pipeline uses a special technique called *pinned purgeables* to keep images off the Java heap. This requires callers to `close` images when they are done with them.  

`SimpleDraweeView` does this for you automatically, so should be your first choice. Very few apps need to use the image pipeline directly.
