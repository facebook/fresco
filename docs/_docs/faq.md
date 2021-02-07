---
docid: faq
title: FAQ
layout: docs
permalink: /docs/faq.html
---

These are common questions asked on our GitHub presence. Please create a pull-request if you have a Q&A that others will profit from.

### How do I clear all caches?

You can use the following code to delete all cached images (both from storage and memory):

```java
// clear both memory and disk caches
Fresco.getImagePipeline().clearCaches();
```

### How can I create a Drawee that supports zoom gestures?

Have a look at the [ZoomableDraweeView](https://github.com/facebook/fresco/tree/master/samples/zoomable) module which is part of our sample code on GitHub.

### How do I create an URI for a local file?

Use the `UriUtil` class:

```java
final File file = new File("your/file/path/img.jpg");
final URI uri = UriUtil.getUriForFile(file);
```

### How do I create an URI for a resource?

Use the `UriUtil` class:

```java
final int resourceId = R.drawable.my_image;
final URI uri = UriUtil.getUriForResourceId(resourceId);

// alternatively, if it is from another package:
final URI uri = UriUtil.getUriForQualifiedResource("com.myapp.plugin", resourceId);
```

### How do I use Fresco in a RecyclerView?

You build your `RecyclerView` just like any other `RecyclerView`. The `DraweeView` is able to attach and detach itself appropriately. When being detached it can free up the memory of the referenced image. When being re-attached, the image is loaded from the BitmapCache if it is still available there.

Have a look at [DraweeRecyclerViewFragment.java](https://github.com/facebook/fresco/blob/1472a3e1b1655e9b52c74e0b06d5ba60d15a42f9/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeRecyclerViewFragment.java) which is part of our showcase app.

### How do I download a image without decoding?

For this, you can use the `imagePipeline#fetchEncodedImage(ImageRequest, ...)` method of the image pipeline. See our section on [Using the Image Pipeline Directly](using-image-pipeline.html) and [DataSources & DataSubscribers](datasources-datasubscribers.html) for detailed samples.

### How do I modify an image before displaying?

The best way is to implement a [PostProcessor](post-processor.html). This allows the image pipeline to schedule the modification on the background and allocates the Bitmaps efficiently.

### How large is Fresco?

If you are correctly following the steps from [Shipping Your App with Fresco](proguard.html), your release builds should not grow more than 500 KiB when adding Fresco.

Adding support for animations (`com.facebook.fresco:animated-gif`, `com.facebook.fresco:animated-webp`) and WebP on old devices (`com.facebook.fresco:webpsupport`) is optional. This modularization allows the base Fresco library to be light-weight. Adding those additional libraries would account for ~100 KiB each.

### Why can’t I use Android’s wrap_content attribute on a DraweeView?

The reason is that Drawee always returns -1 for [getIntrinsicHeight](http://developer.android.com/reference/android/graphics/drawable/Drawable.html#getIntrinsicHeight()) and getIntrinsicWidth methods.

And the reason for that is that unlike a simple ImageView, Drawee may show more than one thing at the same time. For example, during the fade transition from the placeholder to the actual image, both images are visible. There may even be more than one actual image, one low-resolution, the other high-resolution. If all these images are not of exactly the same size, and they practically never are, then the concept of an "intrinsic" size cannot be well defined.

We could have returned the size of the placeholder until the image has finished loading, and then swap to the actual image's size. If we did that, though, the image would not appear correctly - it would be scaled or cropped to the placeholder's size. The only way to prevent that would be to force an Android layout pass when the image loads. Not only will that hurt your app's scroll perf, but it will be jarring for your users, who will suddenly see your app change on screen. Imagine if the user is reading a text article and all of a sudden the text jumps down because the image above it just loaded and caused everything to re-layout.

For this reason, you have to use an actual size or `match_parent` to lay out a DraweeView.

If your images are coming from a server, it may be possible to ask that server for the image dimensions, before you download it. This should be a faster request. Then use [setLayoutParams](http://developer.android.com/reference/android/view/View.html#setLayoutParams(android.view.ViewGroup.LayoutParams)) to dynamically size your view upfront.

If on the other hand your use case is a legitimate exception, you can actually resize Drawee view dynamically by using a controller listener as explained [here](http://stackoverflow.com/a/34075281/3027862). And remember, we intentionally removed this functionality because it is undesireable. [Ugly things should look ugly.](https://youtu.be/qCdpTji8nxo?t=890).
