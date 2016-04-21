---
docid: image-requests
title: Image Requests
layout: docs
permalink: /docs/image-requests.html
prev: modifying-image.html
next: writing-custom-views.html
---

If you need an `ImageRequest` that consists only of a URI, you can use the helper method `ImageRequest.fromURI`. Loading [multiple-images](requesting-multiple-images.html) is a common case of this.

If you need to tell the image pipeline anything more than a simple URI, you need to use `ImageRequestBuilder`:

```java
Uri uri;

ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
    .setBackgroundColor(Color.GREEN)
    .build();

ImageRequest request = ImageRequestBuilder
    .newBuilderWithSource(uri)
    .setImageDecodeOptions(decodeOptions)
    .setAutoRotateEnabled(true)
    .setLocalThumbnailPreviewsEnabled(true)
    .setLowestPermittedRequestLevel(RequestLevel.FULL_FETCH)
    .setProgressiveRenderingEnabled(false)
    .setResizeOptions(new ResizeOptions(width, height))
    .build();
```

#### Fields in ImageRequest

- `uri` - the only mandatory field. See [Supported URIs](supported-uris.html)
- `autoRotateEnabled` - whether to enable [auto-rotation](resizing--rotating.html#rotate).
- `progressiveEnabled` - whether to enable [progressive loading](progressive-jpegs.html).
- `postprocessor` - component to [postprocess](modifying-image.html) the decoded image.
- `resizeOptions` - desired width and height. Use with caution. See [Resizing](resizing-rotating.html).

#### Lowest Permitted Request Level

The image pipeline follows a [definite sequence](intro-image-pipeline.html) in where it looks for the image.

1. Check the bitmap cache. This is nearly instant. If found, return.
2. Check the encoded memory cache. If found, decode the image and return.
3. Check the "disk" (local storage) cache. If found, load from disk, decode, and return.
4. Go to the original file on network or local file. Download, resize and/or rotate if requested, decode, and return. For network images in particular, this will be the slowest by a long shot.

The `setLowestPermittedRequestLevel` field lets you control how far down this list the pipeline will go. Possible values are:

- `BITMAP_MEMORY_CACHE`
- `ENCODED_MEMORY_CACHE`
- `DISK_CACHE`
- `FULL_FETCH`

This is useful in situations where you need an instant, or at least relatively fast, image or none at all.


