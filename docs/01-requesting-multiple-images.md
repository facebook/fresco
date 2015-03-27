---
id: requesting-multiple-images
title: Requesting Multiple Images
layout: docs
permalink: /docs/requesting-multiple-images.html
prev: animations.html
next: listening-download-events.html
---

The methods on this page require [setting your own image request](using-controllerbuilder.html).

### Going from low to high resolution

Suppose you want to show users a high-resolution, slow-to-download image. Rather than let them stare a placeholder for a while, you might want to quickly download a smaller thumbnail first.

You can set two URIs, one for the low-res image, one for the high one:

```java
Uri lowResUri, highResUri;
PipelineDraweeController controller = Fresco.newControllerBuilder()
    .setLowResImageRequest(ImageRequest.fromUri(lowResUri))
    .setImageRequest(ImageRequest.fromUri(highResUri))
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```

### Using thumbnail previews

*This option is supported only for local URIs, and only for images in the JPEG format.*

If your JPEG has a thumbnail stored in its EXIF metadata, the image pipeline can return that as an intermediate result. Your Drawee will first show the thumbnail preview, then the full image when it has finished loading and decoding.

```java
Uri uri;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setLocalThumbnailPreviewsEnabled(true)
    .build();

PipelineDraweeController controller = Fresco.newControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```


### Loading the first available image

Most of the time, an image has no more than one URI. Load it, and you're done.

But suppose you have multiple URIs for the same image. For instance, you might have uploaded an image taken from the camera. Original image would be too big to upload, so the image is downscaled first. In such case, it would be beneficial to first try to get the local-downscaled-uri, then if that fails, try to get the local-original-uri, and if even that fails, try to get the network-uploaded-uri. It would be a shame to download the image that we may have already locally. 

The image pipeline normally searches for images in the memory cache first, then the disk cache, and only then goes out to the network or other source. Rather than doing this one by one for each image, we can have the pipeline check for *all* the images in the memory cache. Only if none were found would disk cache be searched in. Only if none were found there either would an external request be made.

Just create an array of image requests, and pass it to the builder.

```java
Uri uri1, uri2;
ImageRequest request = ImageRequest.fromUri(uri1);
ImageRequest request2 = ImageRequest.fromUri(uri2);
ImageRequest[] requests = { request1, request2 };

PipelineDraweeController controller = Fresco.newControllerBuilder()
    .setFirstAvailableImageRequests(requests)
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```

Only one of the requests will be displayed. The first one found, whether at memory, disk, or network level, will be the one returned. The pipeline will assume the order of requests in the array is the preference order. 
