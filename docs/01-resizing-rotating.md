---
id: resizing-rotating
title: Resizing and Rotating
layout: docs
permalink: /docs/resizing-rotating.html
prev: listening-download-events.html
next: modifying-image.html
---

These features require you to [construct an image request](using-controllerbuilder.html#ImageRequest) directly.

## Resizing Images

### Should you resize?

In general, the order of preference in a URI / display area size difference is:

1. Request a smaller image from your server
2. Scale the image
3. Resize the image.

If your server has a different URI with a smaller version of the image, this is nearly always the best thing to fetch. This avoid wasting your users' data, and saves memory and CPU time in your app.

If your server does not have an alternate URI, or if you are dealing with a local file, the second-best choice is [scaling](scaling.html). Scaling uses Android's own built-in facilities to match the image to the view size. On Android 4.0 and later, this is hardware-accelerated on devices with a GPU. Most of the time, it is the fastest and most effective way to display the image in the size you want. Simply specify the `layout_width` and `layout_height` of your `SimpleDraweeView`, as you would for any Android view. Then specify a [scale type](scaling.html).

However, scaling may be inadvisable when the image is much larger than the area you will display it in, since decoding a large image may result in too large a memory cost. In those cases you can resize.

### What resizing does

Resizing does not modify the original file. Resizing just resizes an encoded image in memory, prior to being decoded.

This can carry out a much greater range of resizing than is possible with Android's facilities. Images taken with the device's camera, in particular, are often much too large to scale and need to be resized before display on the device.

We currently only support resizing for images in the JPEG format. Most Android devices with cameras store files in JPEG.

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

## <a name="rotate"></a>Auto-rotation

It's very annoying to users to see their images show up sideways! Many devices store the orientation of the image in metadata in the JPEG file. If you want images to be automatically rotated to match the device's orientation, you can say so in the image request:

```java
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setAutoRotateEnabled(true)
    .build();
// as above
```
