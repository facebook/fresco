---
docid: resizing
title: Resizing
layout: docs
permalink: /docs/resizing.html
redirect_from:
  - /docs/resizing-rotating.html
---

We use the following terminology for this section:

- **Scaling** is a canvas operation and is usually hardware accelerated. The bitmap itself is always the same size. It just gets drawn downscaled or upscaled. See [ScaleTypes](scaletypes.html).
- **Resizing** is a pipeline operation executed in software. This changes the encoded image in memory before it is being decoded. The decoded bitmap will be smaller than the original image.
- **Downsampling** is also a pipeline operation implemented in software. Rather than creating a new encoded image, it simply decodes only a subset of the pixels, resulting in a smaller output bitmap.

### Resizing

Resizing does not modify the original file, it just resizes an encoded image in memory, prior to being decoded.

To resize pass a `ResizeOptions` object when constructing an `ImageRequest`:

```java
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setResizeOptions(new ResizeOptions(50, 50))
    .build();
mSimpleDraweeView.setController(
    Fresco.newDraweeControllerBuilder()
        .setOldController(mSimpleDraweeView.getController())
        .setImageRequest(request)
        .build());
```

Resizing has some limitations:

- it only supports JPEG files
- the actual resize is carried out to the nearest 1/8 of the original size
- it cannot make your image bigger, only smaller (not a real limitation though)

### Downsampling

Downsampling is an experimental feature added recently to Fresco. To use it, you must explicitly enable it when [configuring the image pipeline](configure-image-pipeline.html):

```java
   .setDownsampleEnabled(true)
```

If this option is on, the image pipeline will downsample your images instead of resizing them. You must still call `setResizeOptions` for each image request as above.

Downsampling is generally faster than resizing, since it is part of the decode step, rather than a separate step of its own. It also supports PNG and WebP (except animated) images as well as JPEG.

The trade-off right now is that, on Android 4.4 (KitKat) it uses more memory than resizing, while the decode is taking place. This should only be an issue for apps decoding a large number of images simultaneously. We hope to find a solution for this and make it the default in a future release.

### Which should you use and when?

If the image is **not** much bigger than the view, then only scaling should be done. It's faster, easier to code, and results in a higher quality output. Of course, images smaller than the view are subset of those **not** much bigger than the view. Therefore, if you need to upscale the image, this should too be done by scaling, and not by resizing. That way memory won't be wasted on a larger bitmap that does not provide any better quality.
However, for images much bigger than the view, such as **local camera images**, resizing in addition to scaling is highly recommended.

As for what exactly "much bigger" means, as a rule of thumb if the image is more than 2 times bigger than the view (in total number of pixels, i.e. width*height), you should resize it. This almost always applies for local images taken by camera. For example, a device with the screen size of 1080 x 1920 pixels (roughly 2MP) and a camera of 16MP produces images 8 times bigger than the display. Without any doubt resizing in such cases is always best to be done.

For network images, try to download an image as close as possible to the size you will be displaying. By downloading images of inappropriate size you are wasting the user's time and data.

If the image is bigger than the view, by not resizing it the memory gets wasted. However, there is also a performance trade-off to be considered.
Clearly, resizing imposes additional CPU cost on its own. But, by not resizing images bigger than the view, more bytes need to be transferred to the GPU, and images get evicted from the bitmap cache more often resulting in more decodes. In other words, not resizing when you should also imposes additional CPU cost.
Therefore, there is no silver bullet and depending on the device characteristics there is a threshold point after which it becomes more performant to go with resize than without it.

### Example

The Fresco showcase app has a [ImagePipelineResizingFragment](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/imagepipeline/ImagePipelineResizingFragment.java) that demonstrates using placeholder, failure and retry images.

![Showcase app with resized example image](/static/images/docs/01-resizing-sample.png)
