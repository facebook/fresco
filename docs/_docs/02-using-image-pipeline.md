---
docid: using-image-pipeline
title: Using the Image Pipeline Directly
layout: docs
permalink: /docs/using-image-pipeline.html
---

This page is intended for advanced usage only. Most apps should be using [Drawees](using-simpledraweeview.html) to interact with Fresco's image pipeline.

Using the image pipeline directly is challenging because of the memory usage. Drawees automatically keep track of whether or not your images need to be in memory. They will swap them out and load them back as soon as they need to be displayed. If you are using the image pipeline directly, your app must repeat this logic.

The image pipeline returns objects wrapped in a [CloseableReference](closeable-references.html). Drawees keep these references alive for as long as they need their image, and then call the `.close()` method on these references when they are finished with them. If your app is not using Drawees, it **must** do the same.

If you do not keep a Java reference to a `CloseableReference` returned by the pipleine, the `CloseableReference` will get garbage collected and the underlying `Bitmap` may get recycled while still being used. If you do not close the `CloseableReference` once you are done with it, you risk memory leaks and OOMs.

To be precise, the Java garbage collector will free image memory when Bitmap objects go out of scope, but this may be too late. Garbage collection is expensive, and relying on it for large objects leads to performance issues. This is especially true on Android 4.x and lower, when Android did not maintain a separate memory space for Bitmaps.

### Calling the pipeline

You must [build an image request](image-requests.html). Having done that, you can pass it directly to the `ImagePipeline:`

```java
ImagePipeline imagePipeline = Fresco.getImagePipeline();
DataSource<CloseableReference<CloseableImage>>
    dataSource = imagePipeline.fetchDecodedImage(imageRequest, callerContext);
```

See the page on [DataSources](datasources-datasubscribers.html) for information on how to receive data from them.

### Skipping the decode

If you don't want to decode the image, but want to get the image bytes in their original compressed format, just use `fetchEncodedImage` instead:

```java
DataSource<CloseableReference<PooledByteBuffer>>
    dataSource = imagePipeline.fetchEncodedImage(imageRequest, callerContext);
```

### Instant results from the bitmap cache

Lookups to the bitmap cache, unlike the others, are done in the UI thread. If a Bitmap is there, you get it instantly.

```java
DataSource<CloseableReference<CloseableImage>> dataSource =
    imagePipeline.fetchImageFromBitmapCache(imageRequest, callerContext);
try {
  CloseableReference<CloseableImage> imageReference = dataSource.getResult();
  if (imageReference != null) {
    try {
      // Do something with the image, but do not keep the reference to it!
      // The image may get recycled as soon as the reference gets closed below.
      // If you need to keep a reference to the image, read the following sections.
    } finally {
      CloseableReference.closeSafely(imageReference);
    }
  } else {
    // cache miss
    ...
  }
} finally {
  dataSource.close();
}
```

### Synchronous image loading

In a similar way to how you can immediately retrieve images from the bitmap cache, it is also possible to load an image from the network synchronously using `DataSources.waitForFinalResult()`.

```java
DataSource<CloseableReference<CloseableImage>> dataSource =
    imagePipeline.fetchImageFromBitmapCache(imageRequest, callerContext);
try {
  CloseableReference<CloseableImage> result = DataSources.waitForFinalResult(dataSource);
  if (result != null) {
    // Do something with the image, but do not keep the reference to it!
    // The image may get recycled as soon as the reference gets closed below.
    // If you need to keep a reference to the image, read the following sections.
  }
} finally {
  dataSource.close();
}
```

Do **not** skip these `finally` blocks!

### The caller Context

As we can see, most of the `ImagePipeline` fetch methods contains a second parameter named `callerContext` of type `Object`. We can see it as an implementation of the [Context Object Design Pattern](https://www.dre.vanderbilt.edu/~schmidt/PDF/Context-Object-Pattern.pdf). It's basically an object we bind to a specific `ImageRequest` that can be used for different purposes (e.g. Log). The same object can also be accessed by all the `Producer` implementations into the `ImagePipeline`.

The caller Context can also be `null`.
