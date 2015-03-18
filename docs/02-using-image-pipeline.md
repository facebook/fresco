---
id: using-image-pipeline
title: Using the Image Pipeline Directly
layout: docs
permalink: /docs/using-image-pipeline.html
prev: caching.html
next: datasources-datasubscribers.html
---

This page is intended for advanced usage only. Most apps should be using [Drawees](using-drawees-xml.html) to interact with Fresco's image pipeline.

Using the image pipeline directly is challenging because of the memory usage. Drawees automatically keep track of whether or not your images need to be in memory. They will swap them out and load them back as soon as they need to be displayed. If you are using the image pipeline directly, your app must repeat this logic.

The image pipeline returns objects wrapped in a [CloseableReference](closeable-references.html). Drawees call the `.close()` method on these objects when they are finished with them. If your app is not using Drawees, it must do the same.

The Java garbage collector will free image memory when Bitmap objects go out of scope, but this may be too late. Garbage collection is expensive, and relying on it for large objects leads to performance issues. This is especially true on Android 4.x and lower, when Android did not maintain a separate memory space for Bitmaps.

#### Calling the pipeline

You must [build an image request](image-requests.html). Having done that, you can pass it directly to the `ImagePipeline:`

```java
ImagePipeline imagePipeline = Fresco.getImagePipeline();
DataSource<CloseableReference<CloseableImage>> 
    dataSource = imagePipeline.fetchDecodedImage(imageRequest);
```

See the page on [DataSources](datasources-datasubscribers.html) for information on how to receive data from them.

#### Skipping the decode

If you don't want to decode the image, but want to keep it in its original compressed format, just use `fetchEncodedImage` instead:

```java
DataSource<CloseableReference<PooledByteBuffer>> 
    dataSource = imagePipeline.fetchEncodedImage(imageRequest);
```

#### Instant results from the bitmap cache

Lookups to the bitmap cache, unlike the others, are done in the UI thread. If a Bitmap is there, you get it instantly. 

```java
DataSource<CloseableReference<CloseableImage>> dataSource =
    mImagePipeline.fetchImageFromBitmapCache(imageRequest);
CloseableReference<CloseableImage> imageReference;
try {
  imageReference = dataSource.getResult();
  if (imageReference != null) {
    CloseableImage image = imageReference.get();
    // do something with the image
  }
} finally {
  dataSource.close();
  CloseableReference.closeSafely(imageReference);
}
```

Do **not** skip this `finally` block!

#### Prefetching 

Prefetching images in advance of showing them can sometimes lead to shorter wait times for users. Remember, however, that there are trade-offs. Prefetching takes up your users' data, and uses up its share of CPU and memory. As a rule, prefetching is not recommended for most apps.

Nonetheless, the image pipeline allows you to prefetch to either disk or bitmap cache. Both will use more data for network URIs, but the disk cache will not do a decode and will therefore use less CPU.

Prefetch to disk:

```java
imagePipeline.prefetchToDiskCache(imageRequest);
```

Prefetch to bitmap cache:

```java
imagePipeline.prefetchToBitmapCache(imageRequest);
```

