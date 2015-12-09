---
id: resizing-rotating
title: Resizing and Rotating
layout: docs
permalink: /docs/resizing-rotating.html
prev: listening-download-events.html
next: modifying-image.html
---

These features require you to [construct an image request](using-controllerbuilder.html#ImageRequest) directly.

### Terminology

- **Scaling** is a canvas operation and is usually hardware accelerated. The bitmap itself is always the same size. It just gets drawn upscaled or downscaled.
- **Resizing** is a pipeline operation executed in software. This changes the encoded image in memory before it is being decoded.
- **Downsampling** is also a pipeline operation implemented in software. Rather than create a new encoded image, it simply decodes only a subset of the pixels, resulting in a smaller output bitmap.

### Which should you use?

Usually scaling. It's faster, easier to code, and results in a higher quality output.

To scale, simply specify the `layout_width` and `layout_height` of your `SimpleDraweeView`, as you would for any Android view. Then specify a [scale type](scaling.html).

Scaling uses Android's own built-in facilities to match the image to the view size. On Android 4.0 and later, this is *hardware-accelerated*, on devices with a GPU. 

The only downside of scaling is that if the image is much bigger than the view, then the memory gets wasted.

### Resizing

Resizing does not modify the original file. Resizing just resizes an encoded image in memory, prior to being decoded.

We recommend using resizing only for local camera images, which on most devices are much larger than the size of a the device's screen.

For network images, try to download an image as close as possible to the size you will be displaying, and then scale it.

To resize pass a [ResizeOptions](../javadoc/reference/com/facebook/imagepipeline/common/ResizeOptions.html) object when constructing an `ImageRequest`:

```java
Uri uri = "file:///mnt/sdcard/MyApp/myfile.jpg";
int width = 50, height = 50;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setResizeOptions(new ResizeOptions(width, height))
    .build();
PipelineDraweeController controller = Fresco.newDraweeControllerBuilder()
    .setOldController(mDraweeView.getController())
    .setImageRequest(request)
    .build();
mSimpleDraweeView.setController(controller);
```

Resizing has a number of limitations:
* it only supports JPEG files
* the actual resize is carried out to the nearest 1/8 of the original size
* it cannot make your image bigger, only smaller
* it will slow down your decodes and possibly the rest of your app, as it's CPU-intensive

### Downsampling

Downsampling is an experimental feature added recently to Fresco. To use it, you must explicitly enable it when [configuring the image pipeline](configure-image-pipeline.html#_):

```java
   .setDownsampleEnabled(true)
```

If this option is on, the image pipeline will downsample your images instead of resizing them. You must still call `setResizeOptions` for each image request as above.

Downsampling is generally faster than resizing, since it is part of the decode step, rather than a separate step of its own. It also supports PNG and WebP (except animated) images as well as JPEG.

The trade-off right now is that, on Android 4.4 (KitKat) it uses more memory than resizing, while the decode is taking place. This should only be an issue for apps decoding a large number of images simultaneously. We hope to find a solution for this and make it the default in a future release.

### <a name="rotate"></a>Auto-rotation

It's very annoying to users to see their images show up sideways! Many devices store the orientation of the image in metadata in the JPEG file. If you want images to be automatically rotated to match the device's orientation, you can say so in the image request:

```java
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setAutoRotateEnabled(true)
    .build();
// as above
```
