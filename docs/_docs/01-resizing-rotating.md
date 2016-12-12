---
docid: resizing-rotating
title: Resizing and Rotating
layout: docs
permalink: /docs/resizing-rotating.html
prev: listening-download-events.html
next: modifying-image.html
---

These features require you to [construct an image request](using-controllerbuilder.html#ImageRequest) directly.

### Terminology

- **Scaling** is a canvas operation and is usually hardware accelerated. The bitmap itself is always the same size. It just gets drawn downscaled or upscaled.
- **Resizing** is a pipeline operation executed in software. This changes the encoded image in memory before it is being decoded. The decoded bitmap will be smaller than the original image.
- **Downsampling** is also a pipeline operation implemented in software. Rather than creating a new encoded image, it simply decodes only a subset of the pixels, resulting in a smaller output bitmap.

### Which should you use and when?

If the image is **not** much bigger than the view, then only scaling should be done. It's faster, easier to code, and results in a higher quality output. Of course, images smaller than the view are subset of those **not** much bigger than the view. Therefore, if you need to upscale the image, this should too be done by scaling, and not by resizing. That way memory won't be wasted on a larger bitmap that does not provide any better quality.
However, for images much bigger than the view, such as **local camera images**, resizing in addition to scaling is higly recommended.

As for what exactly "much bigger" means, as a rule of thumb if the image is more than 2 times biger than the view (in total number of pixels, i.e. width*height), you should resize it. This almost always applies for local images taken by camera. For example, a device with the screen size of 1080 x 1920 pixels (roughly 2MP) and a camera of 16MP produces images 8 times bigger than the display. Without any doubt resizing in such cases is always best to be done.

For network images, try to download an image as close as possible to the size you will be displaying. By downloading images of inappropriate size you are wasting the user's time and data.

If the image is bigger than the view, by not resizing it the memory gets wasted. However, there is also a peformance trade-off to be considered.
Clearly, resizing imposes additional CPU cost on its own. But, by not resizing images bigger than the view, more bytes needs to be transfered to GPU, and images get evicted from the bitmap cache more often resulting in more decodes. In other words, not resizing when you should also imposes additional CPU cost.
Therefore, there is no silver bullet and depending on the device characteristics there is a threshold point after which it becomes more performant to go with resize than without it.

### Scaling

To scale, simply specify the `layout_width` and `layout_height` of your `SimpleDraweeView`, as you would for any Android view. Then specify a [scale type](scaling.html).

Scaling uses Android's own built-in facilities to match the image to the view size. On Android 4.0 and later, this is *hardware-accelerated*, on devices with a GPU.

### Resizing

Resizing does not modify the original file. Resizing just resizes an encoded image in memory, prior to being decoded.

To resize pass a [ResizeOptions](../javadoc/reference/com/facebook/imagepipeline/common/ResizeOptions.html) object when constructing an `ImageRequest`:

```java
Uri uri = "file:///mnt/sdcard/MyApp/myfile.jpg";
int width = 50, height = 50;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setResizeOptions(new ResizeOptions(width, height))
    .build();
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setOldController(mSimpleDraweeView.getController())
    .setImageRequest(request)
    .build();
mSimpleDraweeView.setController(controller);
```

Resizing has some limitations:

- it only supports JPEG files
- the actual resize is carried out to the nearest 1/8 of the original size
- it cannot make your image bigger, only smaller (not a real limitation though)

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
    .setRotationOptions(RotationOptions.autoRotate())
    .build();
// as above
```
