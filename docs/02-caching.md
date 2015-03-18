---
id: caching
title: Caching
layout: docs
permalink: /docs/caching.html
prev: configure-image-pipeline.html
next: using-image-pipeline.html
---

###  The three caches

#### 1. Bitmap cache

The bitmap cache stores Android `Bitmap` objects. These are fully decoded images ready for display or [postprocessing](modifying-image.html). 

On Android 4.x and lower, the bitmap cache's data lives in the *ashmem* heap, not in the Java heap. This means that images don't force extra runs of the garbage collector, slowing down your app.

Android 5.0 has much improved memory management than earlier versions, so it is safer to leave the bitmap cache on the Java heap.

When your app is backgrounded, the bitmap cache is emptied.

#### 2. Encoded memory cache

This cache stores images in their original compressed form. Images retrieved from this cache must be decoded before display. 

If other transformations, such as [resizing, rotating](resizing-rotating.html) or [transcoding](#webp) were requested, that happens before decode.

This cache is also emptied when your app is backgrounded.

#### 3. Disk cache

(Yes, we know phones don't have disks, but it's too tedious to keep saying *local storage cache*...)

Like the encoded memory cache, this cache stores compressed image, which must be decoded and sometimes transformed before display.

Unlike the others, this cache is not cleared when your app is backgrounded, or if it exits, or even if the device is turned off. The user can, of course, always clear it from Android's Settings menu.


### Using one disk cache or two?

Most apps need only a single disk cache. But in some circumstances you may want to keep smaller images in a separate cache, to prevent them from getting evicted too soon by larger images.

To do this, just call both `setMainDiskCacheConfig` and `setSmallImageDiskCacheConfig` methods when [configuring the image pipeline](configure-image-pipeline.html).

What defines *small?* Your app does. When [making an image request](image-requests.html), you set its [ImageType](../javadoc/reference/com/facebook/imagepipeline/request/ImageRequest.ImageType.html):

```java
ImageRequest request = ImageRequest.newBuilderWithSourceUri(uri)
    .setImageType(ImageType.SMALL)
```

If you need only one cache, you can simply avoid calling `setSmallImageDiskCacheConfig`. The pipeline will default to using the same cache for both and `ImageType` will be ignored.

### Trimming the caches

When [configuring](configure-image-pipeline.html) the image pipeline, you can set the maximum size of each of the caches. But there are times when you might want to go lower than that. For instance, your application might have caches for other kinds of data that might need more space and crowd out Fresco's. Or you might be checking to see if the device as a whole is running out of storage space.

Fresco's caches implement the [DiskTrimmable](../javadoc/reference/com/facebook/common/disk/DiskTrimmable.html) or [MemoryTrimmable](../javadoc/reference/com/facebook/common/memory/MemoryTrimmable.html) interfaces. These are hooks into which your app can tell them to do emergency evictions.

Your application can then configure the pipeline with objects implementing the [DiskTrimmableRegistry](../javadoc/reference/com/facebook/common/disk/DiskTrimmableRegistry.html) and [MemoryTrimmableRegistry](../javadoc/reference/com/facebook/common/memory/MemoryTrimmableRegistry.html) interfaces. 

These objects must keep a list of trimmables. They must use app-specific logic to determine when memory or disk space must be preserved. They then notify the trimmable  objects to carry out their trims.
