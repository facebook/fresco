---
docid: rounded-corners-and-circles
title: Rounded Corners and Circles
layout: docs
permalink: /docs/rounded-corners-and-circles.html
---

Not every image is a rectangle. Apps frequently need images that appear with softer, rounded corners, or as circles. Drawee supports a variety of scenarios, all without the memory overhead of copying bitmaps.

### What

Images can be rounded in two shapes:

1. As a circle - set `roundAsCircle` to true.
2. As a rectangle, but with rounded corners. Set `roundedCornerRadius` to some value.

Rectangles support having each of the four corners have a different radius, but this must be specified in Java code rather than XML.

### How

Images can be rounded with two different methods:

1. `BITMAP_ONLY` - Uses a bitmap shader to draw the bitmap with rounded corners. This is the default rounding method. It doesn't support animations, and it does **not** support any scale types other than `centerCrop` (the default), `focusCrop` and `fit_xy`. If you use this rounding method with other scale types, such as `center`, you won't get an Exception but the image might look wrong (e.g. repeated edges due to how Android shaders work), especially in cases the source image is smaller than the view. See the Caveats section below.
2. `OVERLAY_COLOR` - Draws rounded corners by overlaying a solid color, specified by the caller. The Drawee's background should be static and of the same solid color. Use `roundWithOverlayColor` in XML, or `setOverlayColor` in code to use this rounding method.

### In XML

The `SimpleDraweeView` class will forward several attributes over to `RoundingParams`:

```xml
<com.facebook.drawee.view.SimpleDraweeView
  ...
  fresco:roundedCornerRadius="5dp"
  fresco:roundBottomStart="false"
  fresco:roundBottomEnd="false"
  fresco:roundWithOverlayColor="@color/blue"
  fresco:roundingBorderWidth="1dp"
  fresco:roundingBorderColor="@color/red"
>
```

### In code

When constructing a hierarchy, you can pass an instance of [RoundingParams](../javadoc/reference/com/facebook/drawee/generic/RoundingParams.html) to your `GenericDraweeHierarchyBuilder:`

```java
int overlayColor = getResources().getColor(R.color.green);
RoundingParams roundingParams = RoundingParams.fromCornersRadius(7f);
mSimpleDraweeView.setHierarchy(new GenericDraweeHierarchyBuilder(getResources())
        .setRoundingParams(roundingParams)
        .build());
```

You can also change all of the rounding parameters after the hierarchy has been built:

```java
int color = getResources().getColor(R.color.red);
RoundingParams roundingParams = RoundingParams.fromCornersRadius(5f);
roundingParams.setBorder(color, 1.0f);
roundingParams.setRoundAsCircle(true);
mSimpleDraweeView.getHierarchy().setRoundingParams(roundingParams);
```

### Caveats

There are some limitations when `BITMAP_ONLY` (the default) mode is used:

- Only images that resolve to `BitmapDrawable` or `ColorDrawable` can be rounded. Rounding `NinePatchDrawable`, `ShapeDrawable` and other such drawables is not supported (regardless whether they are specified in XML or programmatically).
- Animations are not rounded.
- Due to a limitation of Android's `BitmapShader`, if the image doesn't fully cover the view, instead of drawing nothing, edges are repeated. One workaround is to use a different scale type (e.g. centerCrop) that ensures that the whole view is covered. Another workaround is to make the image file contain a 1px transparent border so that the transparent pixels get repeated. This is the best solution for PNG resource images.

If the limitations of the `BITMAP_ONLY` mode affect your images, see if the `OVERLAY_COLOR` mode works for you. The `OVERLAY_COLOR` mode doesn't have the aforementioned limitations, but since it simulates rounded corners by overlaying a solid color over the image, this only looks good if the background under the view is static and of the same color.

Drawee internally has an implementation for `CLIPPING` mode, but this mode has been disabled and not exposed as some `Canvas` implementation do not support path clipping. Furthermore, canvas clipping doesn't support antialiasing which makes the rounded edges very pixelated.

Finally, all of those issues could be avoided by using a temporary bitmap, but this imposes a significant memory overhead and has not been supported because of that.

As explained above, there is no really good solution for rounding corners on Android and one has to choose between the aforementioned trade-offs.

### Full Sample

For a full sample see the `DraweeRoundedCornersFragment` in the showcase app: [DraweeRoundedCornersFragment.java](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeRoundedCornersFragment.java)

![Showcase app with a scale type example](/static/images/docs/01-rounded-corners-and-circles-sample.png)
