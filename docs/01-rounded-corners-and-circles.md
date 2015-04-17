---
id: rounded-corners-and-circles
title: Rounded Corners and Circles
layout: docs
permalink: /docs/rounded-corners-and-circles.html
prev: scaling.html
next: using-controllerbuilder.html
---

Not every image is a rectangle. Apps frequently need images that appear with softer, rounded corners, or as circles. Drawee supports a variety of scenarios, all without the memory overhead of copying bitmaps.

### What

Images can be rounded in two shapes:

1. As a circle - set `roundAsCircle` to true.
2. As a rectangle, but with rounded corners. Set `roundedCornerRadius` to some value.

Rectangles support having each of the four corners have a different radius, but this must be specified in Java code rather than XML.

### How

Images can be rounded with two different methods:

1. `BITMAP_ONLY` - Uses a shader to draw the bitmap with rounded corners. This is the default rounding method. This works only on the actual image and the [placeholder](drawee-components.html). Other branches, like failure and retry images, are not rounded. Furthermore, this rounding method doesn't support animations.
2. `OVERLAY_COLOR` - Draws rounded corners by overlaying a solid color, specified by the caller. The Drawee's background should be static and of the same solid color. Use `roundWithOverlayColor` in XML, or `setOverlayColor` in code, for this effect.

### In XML

The `SimpleDraweeView` class will forward several attributes over to `RoundingParams`:

```xml
<com.facebook.drawee.view.SimpleDraweeView
   ...
   fresco:roundedCornerRadius="5dp"
   fresco:roundBottomLeft="false"
   fresco:roundBottomRight="false"
   fresco:roundWithOverlayColor="@color/blue"
   fresco:roundingBorderWidth="1dp"
   fresco:roundingBorderColor="@color/red"
```

### In code

When [constructing a hierarchy](using-drawees-code.html), you can pass an instance of [RoundingParams](../javadoc/reference/com/facebook/drawee/generic/RoundingParams.html) to your `GenericDraweeHierarchyBuilder:`

```java
RoundingParams roundingParams = RoundingParams.fromCornersRadius(7f);
roundingParams.setOverlayColor(R.color.green);
// alternatively use fromCornersRadii or asCircle
genericDraweeHierarchyBuilder
    .setRoundingParams(roundingParams);
```

You can also change most of the rounding parameters on the fly:

```java
RoundingParams roundingParams = 
    mSimpleDraweeView.getHierarchy().getRoundingParams();
roundingParams.setBorder(R.color.red, 1.0);
roundingParams.setRoundAsCircle(true);
mSimpleDraweeView.getHierarchy().setRoundingParams(roundingParams);
```

The one exception to this is that the `RoundingMethod` cannot be changed when changing dynamically. Attempting to do so will throw an `IllegalStateException.`


### Caveats

There are some limitations when `BITMAP_ONLY` (the default) mode is used:

- Not all image branches are rounded. Only the placeholder and the actual image are rounded. We are in the process of adding support for rounding backgrounds.
- Only images that resolve to `BitmapDrawable` or `ColorDrawable` can be rounded. Rounding `NinePatchDrawable`, `ShapeDrawable` and other such drawables is not supported (regardless whether they are specified in XML or programmatically).
- Animations are not rounded.
- Due to a limitation of Android's `BitmapShader`, if the image doesn't fully cover the view, isntead of drawing nothing, edges are repeated. One workaround is to use a different scale type (e.g. centerCrop) that ensures that the whole view is covered.

The `OVERLAY_COLOR` mode doesn't have the aforementioned limitations, but since it simulates rounded corners by overlying a solid color over the image, this only looks good if the background under the view is static and of the same color.

Drawee internally has an implementation for `CLIPPING` mode, but this mode has been disabled and not exposed as some `Canvas` implementation do not support path clipping. Furthermore, canvas clipping doesn't support antialiasing which makes the rounded edges very pixelated.

Finally, all of those issues could be avoided by using a temporary bitmap, but this imposes a significant memory overhead and has not been supported because of that.

As explained above, there is no really good solution for rounding corners on Android and one has to choose between the aforementioned trade-offs.
