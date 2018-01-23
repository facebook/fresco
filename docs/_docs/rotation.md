---
docid: rotation
title: Rotation
layout: docs
permalink: /docs/rotation.html
---

You can rotate images by specifying a rotation angle in the image request, like so:

```java
final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(uri)
    .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
    .build();
mSimpleDraweeView.setController(
    Fresco.newDraweeControllerBuilder()
        .setImageRequest(imageRequest)
        .build());
```

### Auto-rotation

JPEG files sometimes store orientation information in the image metadata. If you want images to be automatically rotated to match the device's orientation, you can do so in the image request:

```java
final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(uri)
    .setRotationOptions(RotationOptions.autoRotate())
    .build();
mSimpleDraweeView.setController(
    Fresco.newDraweeControllerBuilder()
        .setImageRequest(imageRequest)
        .build());
```

### Combining rotations

If you're loading a JPEG file that has rotation information in its EXIF data, calling `forceRotation` will **add** to the default rotation of the image. For example, if the EXIF header specifies 90 degrees, and you call `forceRotation(ROTATE_90)`, the raw image will be rotated 180 degrees.

### Examples

The Fresco showcase app has a [DraweeRotationFragment](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeRotationFragment.java) that demonstrates the various rotation settings. You can use it for example with the sample images [from here](https://github.com/recurser/exif-orientation-examples).

![Showcase app with a rotation sample](/static/images/docs/01-rotation-sample.png)
