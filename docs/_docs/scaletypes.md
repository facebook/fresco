---
docid: scaletypes
title: ScaleTypes
layout: docs
permalink: /docs/scaletypes.html
---

You can specify a different scale type for each of the different drawables in your Drawee.

### Available Scale Types

| Scale Type              | Preserves Aspect Ratio | Always Fills Entire View | Performs Scaling | Explanation |
| ---------               | :-:                    | :-:               | :-:              | ----------- |
| center                  | ✓                      |                   |                  | Center the image in the view, but perform no scaling. |
| centerCrop              | ✓                      | ✓                 | ✓                | Scales the image so that both dimensions will be greater than or equal<br/> to the corresponding dimension of the parent. <br/>One of width or height will fit exactly. <br/>The image is centered within parent's bounds. |
| focusCrop               | ✓                      | ✓                 | ✓                | Same as centerCrop, but based around <br/>a caller-specified focus point instead of the center.
| centerInside            | ✓                      |                   | ✓                | Downscales the image so that it fits entirely inside the parent. <br/>Unlike `fitCenter,` no upscaling will be performed. <br/>Aspect ratio is preserved. <br/>The image is centered within parent's bounds. |
| fitCenter               | ✓                      |                   | ✓                | Scales the image so that it fits entirely inside the parent. <br/>One of width or height will fit exactly. <br/>Aspect ratio is preserved. <br/>The image is centered within the parent's bounds. |
| fitStart                | ✓                      |                   | ✓                | Scales the image so that it fits entirely inside the parent. <br/>One of width or height will fit exactly. <br/>Aspect ratio is preserved. <br/>The image is aligned to the top-left corner of the parent.
| fitEnd                  | ✓                      |                   | ✓                | Scales the image so that it fits entirely inside the parent. <br/>One of width or height will fit exactly. <br/>Aspect ratio is preserved. <br/>The image is aligned to the bottom-right corner of the parent.
| fitXY                   |                        | ✓                 | ✓                | Scales width and height independently.<br/> The image will match the parent exactly. <br/>Aspect ratio is not preserved.
| none                    | ✓                      |                   |                  | Used for Android's tile mode. |

These are mostly the same as those supported by the Android [ImageView](http://developer.android.com/reference/android/widget/ImageView.ScaleType.html) class. The one unsupported type is `matrix`. In its place, Fresco offers `focusCrop,` which will usually work better.

### How to Set a Scale Type

ScaleTypes of actual, placeholder, retry, and failure images can all be set in XML, using attributes like `fresco:actualImageScaleType`. You can also set them in code using the [GenericDraweeHierarchyBuilder](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchyBuilder.html) class.

Even after your hierarchy is built, the actual image scale type can be modified on the fly using  [GenericDraweeHierarchy](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchy.html).

However, do **not** use the `android:scaleType` attribute, nor the `.setScaleType` method. These have no effect on Drawees.

### Scale Type: "focusCrop"

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
PointF focusPoint = new PointF(0f, 0.5f);
mSimpleDraweeView
    .getHierarchy()
    .setActualImageFocusPoint(focusPoint);
```

### ScaleType: "none"

If you are using Drawables that make use of Android's tile mode, you need to use the `none` scale type for this to work correctly.

### Scale Type: A Custom ScaleType

Sometimes you need to scale the image in a way that none of the existing scale types does. Drawee allows you to do that easily by implementing your own `ScalingUtils.ScaleType`. There is only one method in that interface, `getTransform`, which is supposed to compute the transformation matrix based on:

* parent bounds (rectangle where the image should be placed in the view's coordinate system)
* child size (width and height of the actual bitmap)
* focus point (relative coordinates in the child's coordinate system)

Of course, your class can contain any additional data you might need to compute the transformation.

Let's look at an example. Assume the `parentBounds` are `(100, 150, 500, 450)`, and the child dimensions are `(420,210)`. Observe that the parent width is `500 - 100 = 400`, and the height is `450 - 150 = 300`. If we don't do any transformation (i.e. we set the transformation to be the identity matrix), the image will be drawn in `(0, 0, 420, 210)`. But `ScaleTypeDrawable` has to respect the bounds imposed by the parent and will so clip the canvas to `(100, 150, 500, 450)`. That means that only the bottom-right part of the image will actually be visible: `(100, 150, 420, 210)`.

We can fix that by doing a translation by `(parentBounds.left, parentBounds.top)`, which is in this case `(100, 150)`. But now the right part of the image got clipped as the image is actually wider than the parent bounds! Image is now placed at `(100, 150, 500, 360)` in the view coordinates, or equivalently `(0, 0, 400, 210)` in the child coordinates. We lost `20` pixels on the right.

To avoid image to be clipped we can downscale it. Here we can scale by `400/420` which will make the image be of the size `(400,200)`.
The image now fits exactly in the view horizontally, but it is not centered in it vertically.

In order to center the image we need to translate it a bit more. We can see that the amount of empty space in the parent bounds is `400 - 400 = 0` horizontally, and `300 - 200 = 100` vertically. If we translate by half of this empty space, we will leave equal amount of empty space on each side, effectively making the image centered in the parent bounds.

Congratulations! You just implemented the `FIT_CENTER` scale type:

```java
  public class AbstractScaleType implements ScaleType {
    @Override
    public Matrix getTransform(Matrix outTransform, Rect parentRect, int childWidth, int childHeight, float focusX, float focusY) {
      // calculate scale; we take the smaller of the horizontal and vertical scale factor so that the image always fits
      final float scaleX = (float) parentRect.width() / (float) childWidth;
      final float scaleY = (float) parentRect.height() / (float) childHeight;
      final float scale = Math.min(scaleX, scaleY);

      // calculate translation; we offset by parent bounds, and by half of the empty space
      // note that the child dimensions need to be adjusted by the scale factor
      final float dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      final float dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;

      // finally, set and return the transform
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
      return outTransform;
    }
  }
```

### Full Sample

For a full sample see the `DraweeScaleTypeFragment` in the showcase app: [DraweeScaleTypeFragment.java](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeScaleTypeFragment.java)

![Showcase app with a scale type example](/static/images/docs/01-scaletypes-sample-1.png)

![Showcase app with a scale type example](/static/images/docs/01-scaletypes-sample-2.png)
