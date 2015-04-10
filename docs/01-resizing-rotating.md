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

### Terminology: resizing vs scaling

- **Resizing** is a pipeline operation executed in software. It returns a completely new bitmap, of a different size.
- **Scaling** is a canvas operation and is usually hardware accelerated. The bitmap itself is always the same size. It just gets drawn upscaled or downscaled.

### Should you resize or scale?

Resizing is rarely necessary. Scaling is almost always preferred, even with resizing.

There are several limitations with resizing:

  - Resize is restricted so that it never returns a bigger image. It can only make the image smaller.
  - At the moment, only JPEG images can be resized.
  - There is only a rough control over the resulting image dimensions. Image cannot be resized to the exact size, but will be reduced by one of the supported resize factors. That means that even resized images need to be scaled before displaying.
  - Only the following resize factors are supported: `N/8` with `1 <= N <= 8`.
  - Resize is performed in software, which is much slower than hardware-accelerated scaling.

Scaling, on the other hand, doesn't suffer any of these limitations. Scaling uses Android's own built-in facilities to match the image to the view size. On Android 4.0 and later, this is hardware-accelerated on devices with a GPU. Most of the time, it is the fastest and most effective way to display the image in the size you want. The only downside is if the image is much bigger than the view, then the memory gets wasted.

Why should you ever use resizing then? It's a trade-off. You should only ever use resize if you need to display an image that is much bigger than the view in order to save memory. One valid example is when you want to display an 8MP photo taken by the camera in a 1280x720 (roughly 1MP) view. An 8MP image would occupy 32MB of memory when decoded to 4 bytes-per-pixel ARGB bitmap. If resized to the view dimensions, it would occupy less than 4 MB.

When it comes to network images, before thinking about resizing, try requesting the image of the proper size first. Don't request an 8MP high-resolution photo from a server if it can return a smaller version. Your users pay for their data plans and you should be considerate of that. Besides, fetching a smaller image saves internal storage and CPU time in your app.

Only if the server doesn't provide an alternate URI with the smaller image, or if you are using local photos, should you resort to resizing. In all other cases, including upscaling the image, scaling should be used. To scale, simply specify the `layout_width` and `layout_height` of your `SimpleDraweeView`, as you would for any Android view. Then specify a [scale type](scaling.html).

### Resizing

Resizing does not modify the original file. Resizing just resizes an encoded image in memory, prior to being decoded.

This can carry out a much greater range of resizing than is possible with Android's facilities. Images taken with the device's camera, in particular, are often much too large to scale and need to be resized before display on the device.

We currently only support resizing for images in the JPEG format, but this is the most widely used image format anyway and most Android devices with cameras store files in the JPEG format.

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
