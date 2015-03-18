---
id: scaling
title: Scaling
layout: docs
permalink: /docs/scaling.html
prev: drawee-components.html
next: rounded-corners-and-circles.html
---

You can specify a different scale type for each of the [different drawables](drawee-components.html) in your Drawee. The 

### Available scale types

| Scale Type | Explanation |
| --------- | ----------- |
| center | Center the image in the view, but perform no scaling. |
| centerCrop | Scales the image so that both dimensions will be greater than or equal to the corresponding dimension of the parent. <br>One of width or height will fit exactly. <br>The image is centered within parent's bounds. |
| [focusCrop](#focusCrop) | Same as centerCrop, but based around a caller-specified focus point instead of the center.
| centerInside | Downscales the image so that it fits entirely inside the parent. <br>Unlike `fitCenter,` no upscaling will be performed. <br>Aspect ratio is preserved. <br>The image is centered within parent's bounds. |
| fitCenter | Scales the image so that it fits entirely inside the parent. <br>One of width or height will fit exactly. <br>Aspect ratio is preserved. <br>The image is centered within the parent's bounds. |
| fitStart | Scales the image so that it fits entirely inside the parent. <br>One of width or height will fit exactly. <br>Aspect ratio is preserved. <br>The image is aligned to the top-left corner of the parent.
| fitEnd | Scales the image so that it fits entirely inside the parent. <br>One of width or height will fit exactly. <br>Aspect ratio is preserved. <br>The image is aligned to the bottom-right corner of the parent.
| fitXY | Scales width and height independently, so that the image matches the parent exactly. <br>Aspect ratio is not preserved.
| [none](#none) | Used for Android's tile mode. |

These are mostly the same as those supported by the Android [ImageView](http://developer.android.com/reference/android/widget/ImageView.ScaleType.html) class.

The one unsupported type is `matrix.` In its place, Fresco offers `focusCrop,` which will usually work better.

### focusCrop

Android, and Fresco, offer a `centerCrop` scale type, which will fill the entire viewing area while preserving the aspect ratio of the image, cropping as necessary.

This is very useful, but the trouble is the cropping doesn't always happen where you need it. If, for instance, you want to crop to someone's face in the bottom right corner of the image, `centerCrop` will do the wrong thing.

By specifying a focus point, you can say which part of the image should be centered in the view. If you specify the focus point to be at the top of the image, such as (0.5f, 0f), we guarantee that, no matter what, this point will be visible and centered in the view as much as possible.

Focus points are specified in a relative coordinate system. That is, (0f, 0f) is the top-left corner, and (1f, 1f) is the bottom-right corner. Relative coordinates allow focus points to be scale-invariant, which is highly useful.

A focus point of (0.5f, 0.5f) is equivalent to a scale type of `centerCrop.`

To use focus points, you must first set the right scale type in your XML:

```xml
  fresco:actualImageScaleType="focusCrop"
```
  
In your Java code, you must programmatically set the correct focus point for your image:
  
```java
PointF focusPoint;
// your app populates the focus point
mSimpleDraweeView
    .getHierarchy()
    .setActualImageFocusPoint(focusPoint);
```

### none

If you are using Drawables that make use of Android's tile mode, you need to use the `none` scale type for this to work correctly.

